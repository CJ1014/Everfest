package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.event.EasterService;
import com.holidayseasons.event.HolidayTrades;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayWindow;
import com.holidayseasons.holiday.OccurrenceRecord;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** In-world tests for Easter, holiday trades, firework shows and Valentine's. Floor y=1, standing height y=2. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpringGameTests {
    private SpringGameTests() {
    }

    private static CelebrationDirector.Festival festival(String id, HolidayPhase phase) {
        EverFestServer s = EverFestServer.require();
        var h = s.holidays().holiday(id).orElseThrow();
        return new CelebrationDirector.Festival(h, new OccurrenceRecord(h.id(), "test", 77, new HolidayWindow(0, 4, 5, 8)), phase);
    }

    @GameTest(template = "plot_16x16")
    public static void hiddenEggsAreFoundExactlyOnce(GameTestHelper helper) {
        EasterService easter = EverFestServer.require().easter();
        String occ = "holidayseasons:easter|test|1";
        BlockPos a = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos b = helper.absolutePos(new BlockPos(11, 2, 11));
        helper.assertTrue(easter.hideEgg(helper.getLevel(), a, occ, "test") && easter.hideEgg(helper.getLevel(), b, occ, "test"), "eggs hidden");
        helper.assertTrue(helper.getLevel().getBlockState(a).isAir(), "hiding an egg places no block");
        List<Interaction> eggs = helper.getLevel().getEntitiesOfClass(Interaction.class, new AABB(a).inflate(12), EasterService::isEgg);
        helper.assertTrue(eggs.size() == 2, "two clickable eggs, found " + eggs.size());
        Interaction first = easter.nearestEgg(helper.getLevel(), a, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack found = easter.collect(player, first);
        helper.assertTrue(!found.isEmpty() && found.is(EverFestItems.EGG_TAG), "the player gets an egg");
        int inInventory = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(EverFestItems.EGG_TAG)) inInventory += s.getCount();
        helper.assertTrue(inInventory == 1, "exactly one egg");
        helper.assertTrue(easter.collect(player, first).isEmpty(), "the same egg cannot be found twice");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class, new AABB(a).inflate(1)).isEmpty(), "egg model gone");
        Villager kid = helper.spawn(EntityType.VILLAGER, new BlockPos(10, 2, 10));
        kid.setAge(-24000 * 5);
        helper.assertTrue(!easter.collect(kid, easter.nearestEgg(helper.getLevel(), b, 3)).isEmpty(), "children find eggs too");
        helper.assertTrue(easter.nearestEgg(helper.getLevel(), b, 12) == null, "no eggs left");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void holidayTradesComeAndGoWithoutTouchingVanillaOffers(GameTestHelper helper) {
        Villager farmer = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 8));
        farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(2));
        List<ItemStack> vanilla = farmer.getOffers().stream().map(MerchantOffer::getResult).map(ItemStack::copy).toList();
        helper.assertTrue(!vanilla.isEmpty(), "farmer has vanilla trades");
        var christmas = festival("holidayseasons:christmas", HolidayPhase.ACTIVE);
        helper.assertTrue(HolidayTrades.apply(farmer, christmas), "holiday trade added");
        helper.assertTrue(!HolidayTrades.apply(farmer, christmas), "only once");
        MerchantOffer special = farmer.getOffers().stream().filter(HolidayTrades::isHolidayOffer).findFirst().orElseThrow();
        helper.assertTrue(special.getResult().is(EverFestItems.ORNAMENT.get()) && special.getCostA().is(Items.EMERALD), "ornaments for emeralds");
        helper.assertTrue(special.getMaxUses() <= 4, "limited uses");
        helper.assertTrue(HolidayTrades.apply(farmer, null), "removed after the holiday");
        List<ItemStack> after = farmer.getOffers().stream().map(MerchantOffer::getResult).toList();
        helper.assertTrue(after.size() == vanilla.size(), "vanilla trades untouched");
        for (int i = 0; i < after.size(); i++) helper.assertTrue(ItemStack.matches(after.get(i), vanilla.get(i)), "same trade " + i);
        Villager nitwit = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 4));
        nitwit.setVillagerData(nitwit.getVillagerData().setProfession(VillagerProfession.NITWIT));
        helper.assertTrue(!HolidayTrades.apply(nitwit, christmas), "nitwits don't trade");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 1000, batch = "everfest_show")
    public static void fireworkShowRunsAndEnds(GameTestHelper helper) {
        var shows = EverFestServer.require().shows();
        shows.start(true);
        helper.assertTrue(shows.running(), "show started");
        helper.succeedWhen(() -> helper.assertTrue(!shows.running(), "show ends on its own"));
    }

    @GameTest(template = "plot_16x16")
    public static void valentinesGiftsOnlyOnValentines(GameTestHelper helper) {
        Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 8));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chocolates = new ItemStack(EverFestItems.CHOCOLATE_BOX.get(), 2);
        var f = EverFestServer.require().director().festival();
        boolean valentines = f != null && f.holiday().id().endsWith("valentines");
        boolean given = EverFestServer.require().gifts().giveValentine(player, v, chocolates);
        helper.assertTrue(given == valentines, "only accepted on Valentine's");
        helper.assertTrue(chocolates.getCount() == (given ? 1 : 2), "nothing consumed otherwise");
        helper.succeed();
    }
}
