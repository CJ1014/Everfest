package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.ai.CelebrationTask;
import com.holidayseasons.ai.DecorateTask;
import com.holidayseasons.ai.GatherTask;
import com.holidayseasons.decoration.DecorationLedger;
import com.holidayseasons.decoration.DecorationPiece;
import com.holidayseasons.decoration.DecorationThemes;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.village.VillageStyle;
import com.holidayseasons.villager.HolidayBrainInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * In-world tests for villager AI integration and decoration safety. Positions are relative to the test's structure
 * block, which sits one block below the template: the floor is y=1 and standing height is y=2.
 */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageGameTests {
    private static final String OCC = "holidayseasons:test|test|1";

    private VillageGameTests() {
    }

    private static Villager idleVillager(GameTestHelper helper, BlockPos pos) {
        Villager v = helper.spawn(EntityType.VILLAGER, pos);
        // An all-IDLE schedule so the test does not depend on the time of day.
        v.getBrain().setSchedule(Schedule.EMPTY);
        v.getBrain().setActiveActivityIfPossible(Activity.IDLE);
        return v;
    }

    @GameTest(template = "plot_16x16")
    public static void brainInjectionKeepsVanillaBehaviours(GameTestHelper helper) {
        Villager reference = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(reference != null && !HolidayBrainInjector.isInjected(reference), "reference villager is untouched");
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 8));
        helper.assertTrue(HolidayBrainInjector.isInjected(v), "spawned villager gets EverFest behaviours");
        for (Activity a : List.of(Activity.IDLE, Activity.MEET, Activity.PLAY)) {
            int vanilla = HolidayBrainInjector.behaviourCount(reference, a);
            int ours = HolidayBrainInjector.behaviourCount(v, a);
            helper.assertTrue(vanilla > 0 || a == Activity.PLAY, a + " has vanilla behaviours");
            helper.assertTrue(ours == vanilla + 1, a + ": expected vanilla " + vanilla + " + 1, got " + ours);
        }
        for (Activity a : List.of(Activity.WORK, Activity.REST, Activity.PANIC, Activity.CORE, Activity.RAID)) {
            helper.assertTrue(HolidayBrainInjector.behaviourCount(reference, a) == HolidayBrainInjector.behaviourCount(v, a), a + " untouched");
        }
        // Injection is idempotent.
        CelebrationDirector director = EverFestServer.require().director();
        HolidayBrainInjector.ensure(v, director);
        helper.assertTrue(HolidayBrainInjector.behaviourCount(v, Activity.IDLE) == HolidayBrainInjector.behaviourCount(reference, Activity.IDLE) + 1, "no double injection");
        // A rebuilt brain (profession change, growing up) is injected again.
        v.refreshBrain(helper.getLevel());
        helper.assertTrue(!HolidayBrainInjector.isInjected(v), "refreshBrain drops our behaviour");
        director.track(v);
        helper.assertTrue(HolidayBrainInjector.isInjected(v), "re-injected after refreshBrain");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 600)
    public static void villagerWalksToFestivalAndFinishes(GameTestHelper helper) {
        Villager v = idleVillager(helper, new BlockPos(2, 2, 2));
        BlockPos target = helper.absolutePos(new BlockPos(12, 2, 12));
        GatherTask task = new GatherTask(OCC, "test", target, target, 40);
        EverFestServer.require().director().assign(v, task);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(v.blockPosition().distManhattan(target) <= 3,
                        "villager should walk to the festival spot, is at " + v.blockPosition() + " target " + target))
                .thenWaitUntil(() -> helper.assertTrue(task.isDone() && EverFestServer.require().director().taskFor(v) == null,
                        "task finishes and is released"))
                .thenExecute(() -> {
                    helper.assertTrue(v.getMainHandItem().isEmpty(), "hands are empty afterwards");
                    helper.assertTrue(v.getBrain().getSchedule() == Schedule.EMPTY, "schedule not owned by EverFest is never changed");
                })
                .thenSucceed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 400)
    public static void panicInterruptsCelebration(GameTestHelper helper) {
        Villager v = idleVillager(helper, new BlockPos(4, 2, 4));
        BlockPos spot = helper.absolutePos(new BlockPos(5, 2, 5));
        GatherTask task = new GatherTask(OCC, "test", spot, spot, 100000);
        EverFestServer.require().director().assign(v, task);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(task.isRunning(), "task starts"))
                .thenExecute(() -> {
                    // A zombie nearby (frozen so the test stays deterministic); vanilla's hostile sensor sees it.
                    var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 2, 4));
                    zombie.setNoAi(true);
                })
                .thenWaitUntil(() -> helper.assertTrue(v.getBrain().isActive(Activity.PANIC), "vanilla panic takes over"))
                .thenWaitUntil(() -> helper.assertTrue(!task.isRunning() && EverFestServer.require().director().taskFor(v) == null,
                        "celebration stops while panicking"))
                .thenSucceed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 600)
    public static void villagerCarriesAndPlacesDecoration(GameTestHelper helper) {
        Villager v = idleVillager(helper, new BlockPos(2, 2, 8));
        DecorationLedger ledger = EverFestServer.require().ledger();
        DecorationPiece piece = DecorationThemes.ground("halloween", VillageStyle.PLAINS).get(0);
        BlockPos spot = helper.absolutePos(new BlockPos(12, 2, 8));
        DecorateTask task = new DecorateTask(OCC, "test", spot, piece, 0, ledger, n -> { });
        EverFestServer.require().director().assign(v, task);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!v.getMainHandItem().isEmpty(), "villager visibly carries the decoration"))
                .thenWaitUntil(() -> helper.assertTrue(task.isDone() && !task.failed(), "decoration placed"))
                .thenExecute(() -> {
                    helper.assertTrue(v.getMainHandItem().isEmpty(), "hands empty after placing");
                    helper.assertTrue(countDisplays(helper, spot) == piece.parts().size(), "display parts spawned");
                    helper.assertTrue(helper.getLevel().getBlockState(spot).is(Blocks.LIGHT), "jack o'lantern glows");
                    helper.assertTrue(helper.getLevel().getBlockState(spot.below()).is(Blocks.SMOOTH_STONE), "floor untouched");
                    ledger.expireOccurrence(OCC);
                    for (int i = 0; i < 50; i++) ledger.tick(helper.getLevel().getServer(), 0, 1000);
                    helper.assertTrue(countDisplays(helper, spot) == 0, "displays removed on cleanup");
                    helper.assertTrue(helper.getLevel().getBlockState(spot).isAir(), "light removed on cleanup");
                })
                .thenSucceed();
    }

    @GameTest(template = "plot_16x16")
    public static void cleanupNeverOverwritesPlayerChanges(GameTestHelper helper) {
        DecorationLedger ledger = EverFestServer.require().ledger();
        DecorationPiece lantern = DecorationThemes.ground("christmas", VillageStyle.PLAINS).get(0);
        BlockPos a = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos b = helper.absolutePos(new BlockPos(9, 2, 9));
        String occ = "holidayseasons:test|safety|1";
        helper.assertTrue(ledger.place(helper.getLevel(), a, lantern, 0, occ, "test") > 0, "placed A");
        helper.assertTrue(ledger.place(helper.getLevel(), b, lantern, 0, occ, "test") > 0, "placed B");
        helper.assertTrue(!ledger.isFreeSpot(helper.getLevel(), a), "occupied spots are not reused");
        // A player builds where decoration A's light was.
        helper.getLevel().setBlock(a, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        ledger.expireOccurrence(occ);
        for (int i = 0; i < 50; i++) ledger.tick(helper.getLevel().getServer(), 0, 1000);
        helper.assertTrue(helper.getLevel().getBlockState(a).is(Blocks.OAK_PLANKS), "player's block survives cleanup");
        helper.assertTrue(helper.getLevel().getBlockState(b).isAir(), "untouched decoration restored to air");
        helper.assertTrue(countDisplays(helper, a) == 0 && countDisplays(helper, b) == 0, "displays gone");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void decorationsNeverReplaceBlocks(GameTestHelper helper) {
        DecorationLedger ledger = EverFestServer.require().ledger();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
        helper.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        helper.assertTrue(!ledger.isFreeSpot(helper.getLevel(), pos), "a block-occupied spot is never free");
        BlockPos floating = helper.absolutePos(new BlockPos(6, 5, 6));
        helper.assertTrue(!ledger.isFreeSpot(helper.getLevel(), floating), "no floating decorations");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void unknownDecorationEntitiesAreRejected(GameTestHelper helper) {
        DecorationLedger ledger = EverFestServer.require().ledger();
        Display.BlockDisplay stray = EntityType.BLOCK_DISPLAY.create(helper.getLevel());
        helper.assertTrue(stray != null, "display created");
        stray.addTag(DecorationLedger.ENTITY_TAG);
        CompoundTag data = new CompoundTag();
        data.putLong("ledger", 987654321L);
        stray.getPersistentData().put(DecorationLedger.ENTITY_TAG, data);
        helper.assertTrue(ledger.shouldRejectEntity(stray, 0), "a decoration the ledger does not know is removed on load");
        Display.BlockDisplay unrelated = EntityType.BLOCK_DISPLAY.create(helper.getLevel());
        helper.assertTrue(!ledger.shouldRejectEntity(unrelated, 0), "other mods'/players' displays are never touched");
        helper.succeed();
    }

    private static int countDisplays(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(pos).inflate(1.5),
                e -> e.getTags().contains(DecorationLedger.ENTITY_TAG)).size();
    }
}
