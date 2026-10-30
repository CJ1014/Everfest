package com.holidayseasons.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * A villager's Christmas wish list: three item ids drawn deterministically from its gift preference (and, for a
 * different year, a different draw). Pure so it can be tested and shown on the client without a server round trip.
 */
public final class WishList {
    public static final Map<VillagerPersonality.GiftPreference, List<String>> CATALOGUE = Map.of(
            VillagerPersonality.GiftPreference.FOOD, List.of("minecraft:cake", "minecraft:pumpkin_pie", "minecraft:cookie", "minecraft:golden_carrot", "minecraft:honey_bottle", "minecraft:rabbit_stew"),
            VillagerPersonality.GiftPreference.BOOKS, List.of("minecraft:book", "minecraft:writable_book", "minecraft:bookshelf", "minecraft:lectern", "minecraft:map", "minecraft:spyglass"),
            VillagerPersonality.GiftPreference.FLOWERS, List.of("minecraft:poppy", "minecraft:sunflower", "minecraft:lily_of_the_valley", "minecraft:flower_pot", "minecraft:pink_petals", "minecraft:azalea"),
            VillagerPersonality.GiftPreference.TOOLS, List.of("minecraft:iron_pickaxe", "minecraft:shears", "minecraft:fishing_rod", "minecraft:iron_axe", "minecraft:lantern", "minecraft:clock"),
            VillagerPersonality.GiftPreference.GEMS, List.of("minecraft:emerald", "minecraft:diamond", "minecraft:amethyst_shard", "minecraft:gold_ingot", "minecraft:lapis_lazuli", "minecraft:quartz"),
            VillagerPersonality.GiftPreference.TREATS, List.of("holidayseasons:candy_corn", "holidayseasons:chocolate_bar", "minecraft:cookie", "minecraft:sweet_berries", "holidayseasons:lollipop", "minecraft:glow_berries"),
            VillagerPersonality.GiftPreference.CLOTHES, List.of("minecraft:leather_helmet", "minecraft:leather_boots", "minecraft:white_wool", "minecraft:red_wool", "minecraft:leather_chestplate", "holidayseasons:santa_hat"));

    private WishList() {
    }

    public static List<String> of(UUID villager, VillagerPersonality.GiftPreference preference, int year) {
        List<String> pool = new ArrayList<>(CATALOGUE.get(preference));
        SplittableRandom r = new SplittableRandom(villager.getLeastSignificantBits() * 31 + villager.getMostSignificantBits() + year * 1_000_003L);
        List<String> out = new ArrayList<>(3);
        for (int i = 0; i < 3 && !pool.isEmpty(); i++) out.add(pool.remove(r.nextInt(pool.size())));
        return List.copyOf(out);
    }

    /** How much a villager likes a present: 2 = on its wish list, 1 = its kind of thing, 0 = a nice thought. */
    public static int appreciation(List<String> wishes, VillagerPersonality.GiftPreference preference, List<String> contents) {
        int best = 0;
        for (String id : contents) {
            if (wishes.contains(id)) return 2;
            if (CATALOGUE.get(preference).contains(id)) best = 1;
        }
        return best;
    }
}
