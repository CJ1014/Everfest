package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.PartyGuestTask;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.decoration.MarketStalls;
import com.holidayseasons.entity.GhostEntity;
import com.holidayseasons.event.HalloweenService;
import com.holidayseasons.event.VillageObjectives;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Tests for village objectives, the graveyard haunting, party guests and festival market stalls. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FestivalGameTests {
    private FestivalGameTests() {
    }

    private static AABB plot(GameTestHelper helper) {
        return new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).expandTowards(16, 8, 16);
    }

    @GameTest(template = "plot_16x16")
    public static void objectivesTakeOnlyWhatIsNeededAndRewardOnce(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        String village = "test|objective|" + UUID.randomUUID();
        VillageObjectives.Progress progress = new VillageObjectives.Progress("minecraft:pumpkin", 10);
        ItemStack wrong = new ItemStack(Items.MELON, 5);
        BlockPos centre = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.assertTrue(s.objectives().contributeTo(helper.getLevel(), p, village, centre, progress, wrong) == 0 && wrong.getCount() == 5,
                "other items are not taken");
        ItemStack pumpkins = new ItemStack(Items.PUMPKIN, 64);
        int taken = s.objectives().contributeTo(helper.getLevel(), p, village, centre, progress, pumpkins);
        helper.assertTrue(taken == 10 && pumpkins.getCount() == 54, "only the 10 needed are taken, took " + taken);
        helper.assertTrue(progress.completed, "objective complete");
        int rep = s.reputation().get(p.getUUID(), village);
        helper.assertTrue(rep >= 10, "contributor earned reputation: " + rep);
        helper.assertTrue(s.objectives().contributeTo(helper.getLevel(), p, village, centre, progress, pumpkins) == 0 && pumpkins.getCount() == 54,
                "a finished objective takes nothing more");
        helper.assertTrue(s.reputation().get(p.getUUID(), village) == rep, "no second reward");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void gravesStirOncePerNight(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        for (int x = 3; x <= 11; x += 4) helper.setBlock(new BlockPos(x, 2, 6), EverFestBlocks.TOMBSTONE.get());
        BlockPos a = helper.absolutePos(new BlockPos(0, 1, 0));
        BoundingBox box = new BoundingBox(a.getX(), a.getY(), a.getZ(), a.getX() + 15, a.getY() + 4, a.getZ() + 15);
        long id = a.asLong() ^ helper.getLevel().getGameTime();
        boolean night = s.halloween().isHalloweenNight();
        int risen = s.haunting().haunt(helper.getLevel(), box, id, null);
        List<GhostEntity> ghosts = helper.getLevel().getEntitiesOfClass(GhostEntity.class, plot(helper).inflate(2));
        int expected = night ? 3 : 0; // Halloween creatures are refused by the world outside Halloween night
        helper.assertTrue(risen == expected && ghosts.size() == expected, "ghosts rose: " + risen + "/" + ghosts.size() + " (night=" + night + ")");
        helper.assertTrue(ghosts.stream().allMatch(g -> g.getTags().contains(HalloweenService.CREATURE_TAG)), "they fade like other Halloween creatures");
        helper.assertTrue(s.haunting().haunt(helper.getLevel(), box, id, null) == 0, "only once per night");
        ghosts.forEach(GhostEntity::discard);
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 600)
    public static void invitedGuestsWalkOverAndLeaveWhenThePartyEnds(GameTestHelper helper) {
        Villager guest = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        BlockPos door = helper.absolutePos(new BlockPos(12, 2, 12));
        long now = helper.getLevel().getGameTime();
        PartyGuestTask task = new PartyGuestTask("test", "test", UUID.randomUUID(), door, now, now + 300, new ItemStack(Items.COOKIE));
        EverFestServer.require().director().assign(guest, task);
        helper.succeedWhen(() -> {
            helper.assertTrue(task.arrived(), "the guest walked to the party");
            // Guests mingle up to 4 blocks from the door before walking back (PartyGuestTask).
            helper.assertTrue(guest.blockPosition().distSqr(door) <= 25, "and stayed at the party");
            helper.assertTrue(task.isDone(), "and goes home when the party ends");
        });
    }

    @GameTest(template = "plot_16x16")
    public static void marketStallsNeedOpenGroundAndAreTracked(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        String occ = "test|market|" + UUID.randomUUID();
        BlockPos near = helper.absolutePos(new BlockPos(8, 2, 8));
        BlockPos at = MarketStalls.place(helper.getLevel(), s.ledger(), near, "holidayseasons:christmas", occ, "test|market", helper.getLevel().getRandom());
        helper.assertTrue(at != null, "a stall fits on the open plot");
        helper.assertTrue(helper.getLevel().getBlockState(at.below()).is(Blocks.SMOOTH_STONE), "on the ground, not on anything else");
        helper.assertTrue(s.ledger().countPieces("test|market", occ, MarketStalls.PIECE_ID) > 0, "recorded in the ledger");
        List<Display.BlockDisplay> parts = helper.getLevel().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(at).inflate(3));
        helper.assertTrue(parts.size() >= 8, "built from displays: " + parts.size());
        helper.assertTrue(MarketStalls.place(helper.getLevel(), s.ledger(), near, "holidayseasons:christmas", occ, "test|market",
                helper.getLevel().getRandom()) == null, "one stall per village per holiday");
        // A crowded spot gets no stall.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) if ((x + z) % 2 == 0) helper.setBlock(new BlockPos(x, 2, z), Blocks.OAK_FENCE);
        helper.assertTrue(MarketStalls.place(helper.getLevel(), s.ledger(), near, "holidayseasons:halloween", occ + "b", "test|market",
                helper.getLevel().getRandom()) == null, "no stall where there is no room");
        s.ledger().expireOccurrence(occ);
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(at).inflate(3)).isEmpty(),
                "removed with the holiday's decorations"));
    }
}
