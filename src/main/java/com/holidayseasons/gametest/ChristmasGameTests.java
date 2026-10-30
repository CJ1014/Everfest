package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.ChristmasTreeBlock;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.block.PresentBlock;
import com.holidayseasons.block.entity.PresentBlockEntity;
import com.holidayseasons.event.GiftService;
import com.holidayseasons.item.EverFestDataComponents;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.item.PresentInfo;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** In-world tests for Christmas gifting. Floor y=1, standing height y=2. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChristmasGameTests {
    private ChristmasGameTests() {
    }

    private static int count(Player p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static PresentBlockEntity wrappedPresent(GameTestHelper helper, BlockPos pos, Player sender, String to) {
        helper.setBlock(pos, EverFestBlocks.PRESENT.get());
        PresentBlockEntity be = (PresentBlockEntity) helper.getBlockEntity(pos);
        be.setOwner(sender.getUUID());
        be.setItem(0, new ItemStack(Items.DIAMOND, 5));
        be.setItem(4, new ItemStack(Items.CAKE));
        be.setInfo(be.info().wrap(sender.getGameProfile().getName(), sender.getUUID(), to, "Merry Christmas", 0x2E8B57));
        helper.setBlock(pos, helper.getBlockState(pos).setValue(PresentBlock.WRAPPED, true));
        return be;
    }

    @GameTest(template = "plot_16x16")
    public static void wrappedPresentOpensOnlyForItsRecipientAndNeverDuplicates(GameTestHelper helper) {
        Player sender = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(8, 2, 8);
        wrappedPresent(helper, pos, sender, "Santa");
        helper.useBlock(pos, stranger);
        helper.assertTrue(count(stranger, Items.DIAMOND) == 0, "a present for someone else stays closed");
        helper.assertBlockPresent(EverFestBlocks.PRESENT.get(), pos);
        // The same present addressed to anyone opens for the stranger, exactly once.
        BlockPos pos2 = new BlockPos(4, 2, 4);
        wrappedPresent(helper, pos2, sender, "");
        helper.useBlock(pos2, stranger);
        helper.assertTrue(count(stranger, Items.DIAMOND) == 5 && count(stranger, Items.CAKE) == 1, "contents handed over");
        helper.assertBlockNotPresent(EverFestBlocks.PRESENT.get(), pos2);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos2)).inflate(2)).isEmpty(),
                "unwrapping drops nothing extra");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void brokenPresentKeepsItsContents(GameTestHelper helper) {
        Player sender = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(8, 2, 8);
        wrappedPresent(helper, pos, sender, "");
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2));
        helper.assertTrue(drops.size() == 1, "one present item drops, not its contents: " + drops.size());
        ItemStack present = drops.get(0).getItem();
        helper.assertTrue(present.is(EverFestItems.PRESENT.get()), "it is a present");
        PresentInfo info = present.get(EverFestDataComponents.PRESENT.get());
        helper.assertTrue(info != null && info.wrapped() && info.message().equals("Merry Christmas"), "wrapping survives");
        long diamonds = 0;
        for (ItemStack s : present.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemsCopy()) {
            if (s.is(Items.DIAMOND)) diamonds += s.getCount();
        }
        helper.assertTrue(diamonds == 5, "contents travel inside the item");
        Player finder = helper.makeMockPlayer(GameType.SURVIVAL);
        finder.setItemInHand(InteractionHand.MAIN_HAND, present.copy());
        present.getItem().use(helper.getLevel(), finder, InteractionHand.MAIN_HAND);
        helper.assertTrue(count(finder, Items.DIAMOND) == 5 && count(finder, EverFestItems.PRESENT.get()) == 0,
                "unwrapping in hand consumes the present and gives its contents exactly once");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void villagersRewardPresentsOncePerChristmas(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 8));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String wish = s.gifts().wishList(v).get(0);
        ItemStack gift = new ItemStack(EverFestItems.PRESENT.get());
        gift.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(wish))))));
        gift.set(EverFestDataComponents.PRESENT.get(), PresentInfo.EMPTY.wrap("Tester", player.getUUID(), "", "", 0xC0392B));
        String village = s.director().resolveVillage(v).key();
        int before = s.reputation().get(player.getUUID(), village);
        ItemStack unwrapped = new ItemStack(EverFestItems.PRESENT.get());
        helper.assertTrue(s.gifts().giveToVillager(player, v, unwrapped) == -1 && unwrapped.getCount() == 1, "an empty, unwrapped box is refused");
        helper.assertTrue(s.gifts().giveToVillager(player, v, gift.copy()) == 2, "a wished-for item delights the villager");
        int after = s.reputation().get(player.getUUID(), village);
        helper.assertTrue(after > before, "reputation rises");
        s.gifts().giveToVillager(player, v, gift.copy());
        helper.assertTrue(s.reputation().get(player.getUUID(), village) == after, "no extra reward for spamming presents");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void villagersGivePlayersOnePresent(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 8));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String village = s.director().resolveVillage(v).key();
        for (int i = 0; i < 40; i++) s.reputation().add(player.getUUID(), village, ReputationMath.Action.COMMUNITY_OBJECTIVE, 1000 + i);
        helper.assertTrue(s.gifts().villagerGivesPresent(v, player), "a liked player receives a present");
        helper.assertTrue(!s.gifts().villagerGivesPresent(v, player), "only once per Christmas");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(2));
        helper.assertTrue(drops.size() == 1, "exactly one present appears");
        ItemStack present = drops.get(0).getItem();
        PresentInfo info = present.get(EverFestDataComponents.PRESENT.get());
        helper.assertTrue(info != null && info.wrapped() && info.to().equals(player.getGameProfile().getName()), "addressed to the player");
        helper.assertTrue(present.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemsCopy().iterator().hasNext(), "not empty");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void christmasTreeDecoratesAndLights(GameTestHelper helper) {
        BlockPos lower = new BlockPos(8, 2, 8);
        var tree = EverFestBlocks.CHRISTMAS_TREE.get().defaultBlockState();
        helper.setBlock(lower, tree);
        helper.setBlock(lower.above(), tree.setValue(ChristmasTreeBlock.HALF, DoubleBlockHalf.UPPER));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EverFestItems.ORNAMENT.get(), 5));
        for (int i = 0; i < 4; i++) helper.useBlock(lower, player);
        helper.assertTrue(helper.getBlockState(lower).getValue(ChristmasTreeBlock.DECORATION) == 3, "three rounds of ornaments max");
        helper.assertTrue(helper.getBlockState(lower.above()).getValue(ChristmasTreeBlock.DECORATION) == 3, "both halves decorated");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "only three ornaments used");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE_DUST));
        helper.useBlock(lower.above(), player);
        helper.assertTrue(helper.getBlockState(lower).getValue(ChristmasTreeBlock.LIT), "lights on");
        helper.assertTrue(helper.getBlockState(lower).getLightEmission(helper.getLevel(), helper.absolutePos(lower)) == 12, "glows");
        helper.getLevel().destroyBlock(helper.absolutePos(lower), false);
        helper.assertBlockNotPresent(EverFestBlocks.CHRISTMAS_TREE.get(), lower.above());
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void festiveHatsAtChristmas(GameTestHelper helper) {
        EverFestServer s = EverFestServer.require();
        Villager kid = helper.spawn(EntityType.VILLAGER, new BlockPos(5, 2, 5));
        kid.setAge(-24000 * 5);
        var h = s.holidays().holiday(GiftService.HOLIDAY_ID).orElseThrow();
        var rec = new com.holidayseasons.holiday.OccurrenceRecord(h.id(), "test", 99, new com.holidayseasons.holiday.HolidayWindow(0, 5, 6, 9));
        var christmas = new com.holidayseasons.ai.CelebrationDirector.Festival(h, rec, com.holidayseasons.holiday.HolidayPhase.ACTIVE);
        s.halloween().updateCostume(kid, christmas, 5);
        ItemStack hat = kid.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        helper.assertTrue(hat.is(EverFestItems.ELF_HAT.get()) || hat.is(EverFestItems.ANTLERS.get()), "children wear elf hats or antlers, got " + hat);
        s.halloween().updateCostume(kid, null, 10);
        helper.assertTrue(kid.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty(), "taken off afterwards");
        helper.succeed();
    }
}
