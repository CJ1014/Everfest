package com.holidayseasons.event;

import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.item.EverFestItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Time-limited holiday trades for existing villagers. While a holiday is celebrating, working villagers add one
 * special offer (paid in emeralds); afterwards it is removed again. Only offers carrying EverFest's marker are ever
 * removed, so vanilla and other mods' trades are untouched. Each offer has few uses, so it cannot be farmed.
 */
public final class HolidayTrades {
    public static final String MARKER = "holidayseasons_holiday_trade";

    private HolidayTrades() {
    }

    @Nullable
    static MerchantOffer offerFor(String holidayId, String occurrenceKey) {
        String path = holidayId.substring(holidayId.indexOf(':') + 1);
        ItemStack result;
        int price;
        switch (path) {
            case "halloween" -> { result = new ItemStack(EverFestItems.CANDY_CORN.get(), 6); price = 1; }
            case "christmas" -> { result = new ItemStack(EverFestItems.ORNAMENT.get(), 4); price = 2; }
            case "easter" -> { result = new ItemStack(EverFestItems.EASTER_BASKET.get()); price = 3; }
            case "new_years" -> { result = new ItemStack(Items.FIREWORK_ROCKET, 6); price = 2; }
            case "valentines" -> { result = new ItemStack(EverFestItems.CHOCOLATE_BOX.get()); price = 2; }
            case "thanksgiving" -> { result = new ItemStack(EverFestItems.HARVEST_PIE.get(), 2); price = 2; }
            case "independence_day" -> { result = new ItemStack(Items.FIREWORK_ROCKET, 8); price = 2; }
            default -> { return null; }
        }
        CustomData.update(DataComponents.CUSTOM_DATA, result, t -> t.putString(MARKER, occurrenceKey));
        return new MerchantOffer(new ItemCost(Items.EMERALD, price), result, 4, 2, 0.05f);
    }

    public static boolean isHolidayOffer(MerchantOffer o) {
        CustomData d = o.getResult().get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains(MARKER);
    }

    /** Adds or removes this villager's holiday offer. Returns true if its offers changed. */
    public static boolean apply(Villager v, @Nullable CelebrationDirector.Festival f) {
        if (v.isBaby()) return false;
        VillagerProfession prof = v.getVillagerData().getProfession();
        boolean working = prof != VillagerProfession.NONE && prof != VillagerProfession.NITWIT;
        MerchantOffers offers = v.getOffers();
        boolean wanted = working && f != null && f.phase() == HolidayPhase.ACTIVE;
        MerchantOffer desired = wanted ? offerFor(f.holiday().id(), f.occurrence().key()) : null;
        String occ = wanted ? f.occurrence().key() : "";
        boolean changed = offers.removeIf(o -> isHolidayOffer(o)
                && (desired == null || !occ.equals(o.getResult().get(DataComponents.CUSTOM_DATA).copyTag().getString(MARKER))));
        if (desired != null && offers.stream().noneMatch(HolidayTrades::isHolidayOffer)) {
            offers.add(desired);
            changed = true;
        }
        return changed;
    }
}
