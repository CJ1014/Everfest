package com.holidayseasons.economy;

import com.holidayseasons.block.CarvedPumpkinVariantBlock;
import com.holidayseasons.entity.HolidayMerchantEntity;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.villager.VillagerPersonality;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * What holiday merchants sell. Each merchant offers a few randomly chosen goods for its occasion, paid in emeralds, plus
 * (with festival tokens enabled) a token-only collectible. Offers have few uses and merchants never restock or buy
 * anything back, so trading cannot be turned into an infinite loop.
 */
public final class MerchantTrades {
    static final int EMERALD_OFFERS = 5;

    record Good(Supplier<ItemStack> stack, int price, int uses) {
        MerchantOffer offer(Item currency) {
            return new MerchantOffer(new ItemCost(currency, price), stack.get(), uses, 1, 0.05f);
        }
    }

    private MerchantTrades() {
    }

    private static Good g(Item item, int count, int price, int uses) {
        return new Good(() -> new ItemStack(item, count), price, uses);
    }

    private static Item costume(VillagerPersonality.Costume c) {
        return EverFestItems.COSTUMES.get(c).get();
    }

    static List<Good> goods(String occasion) {
        String path = occasion.substring(occasion.indexOf(':') + 1);
        List<Good> list = new ArrayList<>();
        switch (path) {
            case "christmas" -> {
                list.add(g(EverFestItems.ORNAMENT.get(), 4, 1, 6));
                list.add(g(EverFestItems.WREATH.get(), 1, 2, 4));
                list.add(g(EverFestItems.STRING_LIGHTS.get(), 4, 2, 4));
                list.add(g(EverFestItems.CHRISTMAS_TREE.get(), 1, 4, 2));
                list.add(g(EverFestItems.SANTA_HAT.get(), 1, 3, 2));
                list.add(g(EverFestItems.ELF_HAT.get(), 1, 3, 2));
                list.add(g(EverFestItems.ANTLERS.get(), 1, 3, 2));
                list.add(g(EverFestItems.PRESENT.get(), 1, 1, 4));
                list.add(g(Items.COOKIE, 8, 1, 6));
            }
            case "halloween" -> {
                for (VillagerPersonality.Costume c : VillagerPersonality.Costume.values()) list.add(g(costume(c), 1, 2, 2));
                list.add(g(EverFestItems.CANDY_BAG.get(), 1, 2, 3));
                list.add(g(EverFestItems.CANDY_BOWL.get(), 1, 2, 3));
                list.add(g(EverFestItems.DOORBELL.get(), 1, 2, 3));
                list.add(g(EverFestItems.JACK.get(CarvedPumpkinVariantBlock.Face.CAT).get(), 1, 2, 3));
                list.add(g(EverFestItems.HANGING_GHOST.get(), 2, 2, 3));
                list.add(g(EverFestItems.CANDY_CORN.get(), 6, 1, 6));
            }
            case HolidayMerchantEntity.MIDNIGHT -> {
                list.add(g(EverFestItems.ECTOPLASM.get(), 3, 1, 4));
                list.add(g(costume(VillagerPersonality.Costume.GHOST), 1, 2, 2));
                list.add(g(costume(VillagerPersonality.Costume.SKELETON), 1, 2, 2));
                list.add(g(costume(VillagerPersonality.Costume.VAMPIRE), 1, 2, 2));
                list.add(g(EverFestItems.JACK.get(CarvedPumpkinVariantBlock.Face.SCARY).get(), 1, 2, 3));
                list.add(g(EverFestItems.TOMBSTONE.get(), 1, 1, 3));
                list.add(g(Items.SOUL_LANTERN, 2, 2, 3));
                list.add(g(Items.CANDLE, 4, 1, 4));
            }
            case "easter" -> {
                list.add(g(EverFestItems.EASTER_EGG_PINK.get(), 4, 1, 6));
                list.add(g(EverFestItems.EASTER_EGG_BLUE.get(), 4, 1, 6));
                list.add(g(EverFestItems.EASTER_EGG_YELLOW.get(), 4, 1, 6));
                list.add(g(EverFestItems.EASTER_EGG_GREEN.get(), 4, 1, 6));
                list.add(g(EverFestItems.EASTER_BASKET.get(), 1, 3, 2));
                list.add(g(EverFestItems.CHOCOLATE_BOX.get(), 1, 2, 4));
                list.add(g(Items.DANDELION, 6, 1, 4));
                list.add(g(Items.AZURE_BLUET, 6, 1, 4));
            }
            case "valentines" -> {
                list.add(g(EverFestItems.CHOCOLATE_BOX.get(), 1, 2, 4));
                list.add(g(Items.POPPY, 8, 1, 4));
                list.add(g(Items.ROSE_BUSH, 2, 1, 4));
                list.add(g(Items.PINK_WOOL, 8, 1, 4));
                list.add(g(Items.CAKE, 1, 3, 2));
                list.add(g(Items.PINK_TULIP, 6, 1, 4));
            }
            case "thanksgiving" -> {
                list.add(g(EverFestItems.HARVEST_PIE.get(), 2, 2, 4));
                list.add(g(EverFestItems.ROAST_TURKEY.get(), 3, 2, 4));
                list.add(g(Items.PUMPKIN, 4, 1, 4));
                list.add(g(Items.HAY_BLOCK, 2, 1, 4));
                list.add(g(Items.APPLE, 6, 1, 4));
                list.add(g(Items.BREAD, 6, 1, 4));
            }
            default -> { // new year's, independence day and any data-pack holiday
                list.add(g(Items.FIREWORK_ROCKET, 8, 2, 4));
                list.add(g(Items.LANTERN, 2, 1, 4));
                list.add(g(Items.CAKE, 1, 3, 2));
                list.add(g(Items.RED_BANNER, 1, 1, 3));
                list.add(g(Items.WHITE_BANNER, 1, 1, 3));
                list.add(g(Items.BLUE_BANNER, 1, 1, 3));
            }
        }
        return list;
    }

    /** The token-only collectible for an occasion. */
    static Good collectible(String occasion) {
        String path = occasion.substring(occasion.indexOf(':') + 1);
        return switch (path) {
            case "christmas", "new_years" -> g(EverFestItems.SNOW_GLOBE.get(), 1, 5, 1);
            case "easter" -> g(EverFestItems.GOLDEN_EGG.get(), 1, 3, 2);
            case "halloween", HolidayMerchantEntity.MIDNIGHT -> new Good(MerchantTrades::peddlersLantern, 4, 1);
            case "thanksgiving" -> g(EverFestItems.HARVEST_PIE.get(), 4, 2, 2);
            case "valentines" -> g(EverFestItems.CHOCOLATE_BOX.get(), 3, 2, 2);
            default -> g(Items.FIREWORK_ROCKET, 24, 2, 2);
        };
    }

    /** A collectible lantern only the Midnight Peddler and Halloween merchants sell, for tokens. */
    public static ItemStack peddlersLantern() {
        ItemStack s = new ItemStack(Items.SOUL_LANTERN);
        s.set(DataComponents.CUSTOM_NAME, Component.translatable("item.holidayseasons.peddlers_lantern"));
        s.set(DataComponents.RARITY, Rarity.EPIC);
        s.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.holidayseasons.peddlers_lantern.lore"))));
        return s;
    }

    /** {@link #EMERALD_OFFERS} random goods for emeralds, plus the token collectible when tokens are enabled. */
    public static List<MerchantOffer> offersFor(String occasion, boolean tokens, RandomSource random) {
        List<MerchantOffer> out = new ArrayList<>();
        if (occasion.isEmpty()) return out;
        List<Good> pool = new ArrayList<>(goods(occasion));
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        for (int i = 0; i < Math.min(EMERALD_OFFERS, pool.size()); i++) out.add(pool.get(i).offer(Items.EMERALD));
        if (tokens) out.add(collectible(occasion).offer(EverFestItems.HOLIDAY_TOKEN.get()));
        return out;
    }
}
