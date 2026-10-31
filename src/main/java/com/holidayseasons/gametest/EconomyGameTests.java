package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.economy.HolidayTokens;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.HolidayMerchantEntity;
import com.holidayseasons.item.EverFestItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests for holiday merchants, the Midnight Peddler and festival tokens. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EconomyGameTests {
    private EconomyGameTests() {
    }

    @GameTest(template = "plot_16x16")
    public static void merchantOffersAreLimitedAndNeverBuyBack(GameTestHelper helper) {
        for (String occasion : new String[]{"holidayseasons:christmas", "holidayseasons:halloween", HolidayMerchantEntity.MIDNIGHT,
                "holidayseasons:easter", "holidayseasons:valentines", "holidayseasons:thanksgiving", "holidayseasons:new_years"}) {
            HolidayMerchantEntity m = helper.spawn(EverFestEntities.HOLIDAY_MERCHANT.get(), new BlockPos(8, 2, 8));
            m.setNoAi(true);
            m.setOccasion(occasion);
            int expected = 5 + (HolidayTokens.enabled() ? 1 : 0);
            helper.assertTrue(m.getOffers().size() == expected, occasion + ": " + m.getOffers().size() + " offers");
            for (MerchantOffer o : m.getOffers()) {
                boolean paidInEmeralds = o.getBaseCostA().is(Items.EMERALD);
                boolean paidInTokens = o.getBaseCostA().is(EverFestItems.HOLIDAY_TOKEN.get());
                helper.assertTrue(paidInEmeralds || paidInTokens, occasion + " offer paid in emeralds or tokens");
                helper.assertTrue(o.getCostB().isEmpty(), "single cost");
                helper.assertTrue(!o.getResult().is(Items.EMERALD) && !o.getResult().is(EverFestItems.HOLIDAY_TOKEN.get()), "never pays out currency");
                helper.assertTrue(o.getMaxUses() <= 6, "limited uses");
            }
            if (occasion.endsWith("christmas")) {
                helper.assertTrue(m.getItemBySlot(EquipmentSlot.HEAD).is(EverFestItems.SANTA_HAT.get()), "the Christmas merchant wears a Santa hat");
            }
            m.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 250)
    public static void merchantsLeaveWhenTheirHolidayIsOver(GameTestHelper helper) {
        HolidayMerchantEntity m = helper.spawn(EverFestEntities.HOLIDAY_MERCHANT.get(), new BlockPos(8, 2, 8));
        m.setOccasion("holidayseasons:no_such_holiday");
        helper.assertTrue(m.timeToLeave(), "nothing to celebrate");
        HolidayMerchantEntity peddler = helper.spawn(EverFestEntities.HOLIDAY_MERCHANT.get(), new BlockPos(4, 2, 4));
        peddler.setOccasion(HolidayMerchantEntity.MIDNIGHT);
        long tod = helper.getLevel().getDayTime() % 24000L;
        helper.assertTrue(peddler.timeToLeave() == (tod < 13000 || tod >= 23000), "the peddler only stays at night");
        peddler.discard();
        helper.succeedWhen(() -> helper.assertTrue(m.isRemoved(), "merchant left"));
    }

    @GameTest(template = "plot_16x16")
    public static void tokensOnlyWhenEnabled(GameTestHelper helper) {
        Player p = helper.makeMockPlayer(GameType.SURVIVAL);
        int given = HolidayTokens.award(p, 2, "holidayseasons.tokens.reason.egg");
        int held = p.getInventory().countItem(EverFestItems.HOLIDAY_TOKEN.get());
        helper.assertTrue(given == (HolidayTokens.enabled() ? 2 : 0) && held == given, "given " + given + ", held " + held);
        helper.succeed();
    }
}
