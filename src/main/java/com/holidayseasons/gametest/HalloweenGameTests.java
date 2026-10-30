package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.ai.TrickOrTreatGroup;
import com.holidayseasons.ai.TrickOrTreatTask;
import com.holidayseasons.block.CandyBowlBlock;
import com.holidayseasons.block.CarvedPumpkinVariantBlock;
import com.holidayseasons.block.DoorbellBlock;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.block.entity.CandyBowlBlockEntity;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.event.HalloweenService;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.holiday.OccurrenceRecord;
import com.holidayseasons.holiday.ScheduledHoliday;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.item.CostumeItem;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

/** In-world tests for the Halloween release. Floor at y=1, standing height y=2 (relative to the structure block). */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HalloweenGameTests {
    private HalloweenGameTests() {
    }

    private static Villager child(GameTestHelper helper, BlockPos pos) {
        Villager v = helper.spawn(EntityType.VILLAGER, pos);
        v.setAge(-24000 * 5);
        v.getBrain().setSchedule(Schedule.EMPTY);
        v.getBrain().setActiveActivityIfPossible(Activity.IDLE);
        return v;
    }

    @GameTest(template = "plot_16x16")
    public static void candyBowlLimitsVisitorsAndBlocksDraining(GameTestHelper helper) {
        BlockPos pos = new BlockPos(8, 2, 8);
        helper.setBlock(pos, EverFestBlocks.CANDY_BOWL.get());
        CandyBowlBlockEntity bowl = (CandyBowlBlockEntity) helper.getBlockEntity(pos);
        bowl.setOwner(UUID.randomUUID());
        ItemStack rest = bowl.add(new ItemStack(EverFestItems.CANDY_CORN.get(), 10));
        helper.assertTrue(rest.isEmpty(), "candy fits");
        helper.assertTrue(bowl.add(new ItemStack(Items.DIRT)).getCount() == 1, "only candy goes in");
        helper.assertTrue(helper.getBlockState(pos).getValue(CandyBowlBlock.FILL) == 2, "fill level reflects 10 candies");

        IItemHandler automation = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(automation != null, "bowl exposes an item handler");
        helper.assertTrue(automation.extractItem(0, 64, false).isEmpty(), "hoppers cannot drain a bowl");
        helper.assertTrue(automation.insertItem(1, new ItemStack(EverFestItems.LOLLIPOP.get(), 2), false).isEmpty(), "hoppers can refill a bowl");

        Player visitor = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(pos, visitor);
        helper.assertTrue(visitor.getInventory().countItem(EverFestItems.CANDY_CORN.get()) + visitor.getInventory().countItem(EverFestItems.LOLLIPOP.get()) == 1,
                "a visitor takes one candy");
        // Empty hand again (the candy went to the selected slot; holding candy would refill the bowl instead).
        visitor.getInventory().selected = 5;
        helper.useBlock(pos, visitor);
        helper.assertTrue(visitor.getInventory().countItem(EverFestItems.CANDY_CORN.get()) + visitor.getInventory().countItem(EverFestItems.LOLLIPOP.get()) == 1,
                "but only one per day");
        helper.assertTrue(bowl.candyCount() == 11, "bowl lost exactly one candy, has " + bowl.candyCount());

        ItemStack served = CandyBowlBlock.serveVillager(helper.getLevel(), helper.absolutePos(pos), "test");
        helper.assertTrue(!served.isEmpty() && bowl.candyCount() == 10, "village children are served");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void emptyBowlGivesFeedbackNotCandy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        helper.setBlock(pos, EverFestBlocks.CANDY_BOWL.get());
        Player visitor = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(pos, visitor);
        helper.assertTrue(visitor.getInventory().countItem(EverFestItems.CANDY_CORN.get()) == 0, "nothing from an empty bowl");
        helper.assertTrue(CandyBowlBlock.serveVillager(helper.getLevel(), helper.absolutePos(pos), "test").isEmpty(), "children get nothing either");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 80)
    public static void doorbellRingsAndPulsesRedstone(GameTestHelper helper) {
        BlockPos wall = new BlockPos(8, 2, 9);
        BlockPos bell = new BlockPos(8, 2, 8);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(bell, EverFestBlocks.DOORBELL.get().defaultBlockState()
                .setValue(DoorbellBlock.FACE, AttachFace.WALL).setValue(DoorbellBlock.FACING, Direction.NORTH));
        BlockState state = helper.getBlockState(bell);
        DoorbellBlock.ring(helper.getLevel(), helper.absolutePos(bell), state, null, null);
        helper.assertTrue(helper.getBlockState(bell).getValue(DoorbellBlock.POWERED), "pressed while ringing");
        helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(wall)), "powers the block it is mounted on");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!helper.getBlockState(bell).getValue(DoorbellBlock.POWERED), "released after the pulse");
            helper.succeed();
        });
    }

    @GameTest(template = "plot_16x16")
    public static void pumpkinCarvingAndLighting(GameTestHelper helper) {
        BlockPos pos = new BlockPos(8, 2, 8);
        helper.setBlock(pos, Blocks.PUMPKIN);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EverFestItems.CARVING_KNIFE.get()));
        helper.useBlock(pos, player);
        BlockState carved = helper.getBlockState(pos);
        helper.assertTrue(carved.getBlock() instanceof CarvedPumpkinVariantBlock c && c.face() == CarvedPumpkinVariantBlock.Face.GRIN && !c.lit(),
                "a pumpkin carves into a grin, got " + carved);
        helper.useBlock(pos, player);
        helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof CarvedPumpkinVariantBlock c && c.face() == CarvedPumpkinVariantBlock.Face.SCARY,
                "carving again changes the face");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH, 3));
        helper.useBlock(pos, player);
        BlockState lit = helper.getBlockState(pos);
        helper.assertTrue(lit.getBlock() instanceof CarvedPumpkinVariantBlock c && c.lit() && c.face() == CarvedPumpkinVariantBlock.Face.SCARY,
                "a torch lights it, got " + lit);
        helper.assertTrue(lit.getLightEmission(helper.getLevel(), helper.absolutePos(pos)) == 15, "jack-o'-lanterns glow");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "the torch is used up");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void hearthRegistrationRules(GameTestHelper helper) {
        var homes = EverFestServer.require().homes();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        BlockPos hearth = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos door = new BlockPos(3, 2, 8);
        helper.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(homes.validate(alice, helper.getLevel().dimension(), hearth) == null, "first home is fine");
        HomeRecord home = homes.register(helper.getLevel(), alice, "Alice", hearth);
        try {
            BlockPos absDoor = helper.absolutePos(door);
            helper.assertTrue(home.entrance().distManhattan(absDoor) == 1, "entrance is next to the door, got " + home.entrance());
            helper.assertTrue(home.entrance().distSqr(hearth) > absDoor.distSqr(hearth), "entrance is the outside of the door");
            helper.assertTrue("holidayseasons.home.too_close".equals(homes.validate(bob, helper.getLevel().dimension(), hearth.offset(4, 0, 0))),
                    "another player cannot build a hearth right next door");
            HomeRecord second = homes.register(helper.getLevel(), alice, "Alice", hearth.offset(0, 0, 100));
            helper.assertTrue("holidayseasons.home.too_many".equals(homes.validate(alice, helper.getLevel().dimension(), hearth.offset(500, 0, 0))),
                    "home limit per player is enforced");
            homes.unregister(second.id);
            helper.assertTrue(homes.blocksVillagerDecorations(helper.getLevel().dimension(), hearth.offset(2, 0, 2)), "villagers keep out of homes");
            home.applySettings("Alice's Spooky House", true, true, HomeRecord.CandyPolicy.OWNER_ONLY);
            helper.assertTrue(!homes.blocksVillagerDecorations(helper.getLevel().dimension(), hearth.offset(2, 0, 2)), "unless the owner allows it");
            home.applySettings("   ", true, false, HomeRecord.CandyPolicy.EVERYONE);
            helper.assertTrue(home.name().equals("Alice's Spooky House"), "blank names are ignored");
        } finally {
            homes.unregister(home.id);
        }
        helper.succeed();
    }

    private static CelebrationDirector.Festival halloween(HolidayPhase phase) {
        EverFestServer s = EverFestServer.require();
        ScheduledHoliday h = s.holidays().holiday(HalloweenService.HOLIDAY_ID).orElseThrow();
        OccurrenceRecord rec = new OccurrenceRecord(h.id(), "test", 99, new HolidayWindow(0, 4, 5, 8));
        return new CelebrationDirector.Festival(h, rec, phase);
    }

    @GameTest(template = "plot_16x16")
    public static void costumesAreTemporaryAndNeverStealHats(GameTestHelper helper) {
        HalloweenService service = EverFestServer.require().halloween();
        Villager kid = child(helper, new BlockPos(4, 2, 4));
        Villager adultWithHat = helper.spawn(EntityType.VILLAGER, new BlockPos(10, 2, 10));
        ItemStack ownHat = new ItemStack(Items.LEATHER_HELMET);
        adultWithHat.setItemSlot(EquipmentSlot.HEAD, ownHat);
        CelebrationDirector.Festival active = halloween(HolidayPhase.ACTIVE);
        service.updateCostume(kid, active, 4);
        service.updateCostume(adultWithHat, active, 4);
        ItemStack costume = kid.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(costume.getItem() instanceof CostumeItem c
                && c.costume() == VillagerPersonality.of(kid.getUUID(), "none", true).costume(), "children wear their favourite costume");
        helper.assertTrue(CostumeItem.isTemporary(costume), "villager costumes are marked temporary");
        helper.assertTrue(adultWithHat.getItemBySlot(EquipmentSlot.HEAD) == ownHat, "an existing hat is never replaced");
        service.updateCostume(kid, null, 9);
        service.updateCostume(adultWithHat, null, 9);
        helper.assertTrue(kid.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "costume comes off after Halloween");
        helper.assertTrue(adultWithHat.getItemBySlot(EquipmentSlot.HEAD) == ownHat, "and other hats stay on");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 1200)
    public static void childrenTrickOrTreatAtARegisteredHome(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        HalloweenService service = s.halloween();
        BlockPos hearthRel = new BlockPos(8, 2, 2);
        BlockPos doorRel = new BlockPos(8, 2, 6);
        helper.setBlock(hearthRel, EverFestBlocks.HOLIDAY_HEARTH.get());
        helper.setBlock(doorRel, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(doorRel.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        HomeRecord home = s.homes().register(helper.getLevel(), UUID.randomUUID(), "Owner", helper.absolutePos(hearthRel));
        BlockPos bowlRel = new BlockPos(10, 2, 8);
        helper.setBlock(bowlRel, EverFestBlocks.CANDY_BOWL.get());
        CandyBowlBlockEntity bowl = (CandyBowlBlockEntity) helper.getBlockEntity(bowlRel);
        bowl.add(new ItemStack(EverFestItems.CHOCOLATE_BAR.get(), 6));
        Villager leader = child(helper, new BlockPos(3, 2, 13));
        Villager friend = child(helper, new BlockPos(4, 2, 14));
        String village = "test|tot|" + UUID.randomUUID();
        TrickOrTreatGroup group = new TrickOrTreatGroup(leader);
        group.members.add(friend);
        TrickOrTreatTask task = new TrickOrTreatTask("holidayseasons:halloween|test|" + UUID.randomUUID(), village, service, group, 3);
        s.director().assign(leader, task);
        s.director().assign(friend, new com.holidayseasons.ai.FollowGroupTask(task.occurrenceKey, village, group));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(group.current != null && group.current.homeId() != null && group.current.homeId().equals(home.id),
                        "the group heads for the registered home (running " + task.isRunning() + ", done " + task.isDone() + ", failed " + task.failed()
                                + ", phase " + group.phase + ", current " + group.current + ", visited " + group.visited
                                + ", activity " + leader.getBrain().getActiveNonCoreActivity() + ", baby " + leader.isBaby()
                                + ", injected " + com.holidayseasons.villager.HolidayBrainInjector.isInjected(leader) + ")"))
                .thenWaitUntil(() -> helper.assertTrue(group.phase == TrickOrTreatGroup.Phase.WAITING || group.treats > 0, "they knock and wait"))
                .thenWaitUntil(() -> helper.assertTrue(group.treats == 1, "the candy bowl answers"))
                .thenWaitUntil(() -> helper.assertTrue(task.isDone(), "no other doors: the group goes home"))
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(bowl.candyCount() == 4, "one candy per child, bowl has " + bowl.candyCount());
                        helper.assertTrue(group.visited.contains("home:" + home.id), "the visit is remembered");
                        helper.assertTrue(home.visitsThisYear() == 1, "the home counts the visit");
                        helper.assertTrue(leader.getMainHandItem().isEmpty(), "hands empty after the reaction");
                        helper.assertTrue(s.reputation().get(bowl.owner() == null ? home.owner : bowl.owner(), village) >= 0, "reputation tracked");
                    } finally {
                        s.homes().unregister(home.id);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "plot_16x16")
    public static void halloweenCreaturesOnlyOnHalloweenNight(GameTestHelper helper) {
        HalloweenService service = EverFestServer.require().halloween();
        var ghost = helper.spawn(EverFestEntities.GHOST.get(), new BlockPos(8, 4, 8));
        helper.assertTrue(!service.shouldRejectCreature(ghost), "untagged (e.g. spawn-egg) ghosts are left alone");
        ghost.addTag(HalloweenService.CREATURE_TAG);
        helper.assertTrue(service.isHalloweenNight() || service.shouldRejectCreature(ghost), "Halloween creatures vanish outside Halloween night");
        ghost.discard();
        helper.succeed();
    }
}
