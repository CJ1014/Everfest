package com.holidayseasons.reputation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Holiday Collection Book's catalogue: what can be collected, discovered, achieved and encountered. Pure data so
 * client and server agree on it and it can be unit-tested.
 */
public final class Collectibles {
    /** Collectible items by page section, as item ids (the peddler's lantern is a named soul lantern: special id). */
    public static final Map<String, List<String>> ITEMS = new LinkedHashMap<>();
    public static final String PEDDLERS_LANTERN = "holidayseasons:peddlers_lantern";
    /** Wildlife entries: species, or species.variant. */
    public static final List<String> WILDLIFE = List.of("reindeer.brown", "reindeer.frosty", "reindeer.festive",
            "spring_bunny.cream", "spring_bunny.blush", "spring_bunny.sky", "spring_bunny.lilac", "wild_turkey");
    /** Special encounters. */
    public static final List<String> ENCOUNTERS = List.of("holiday_merchant", "midnight_peddler", "graveyard_haunting",
            "mysterious_gift", "firework_show", "trick_or_treaters", "hearth_party");
    /** Festival achievements tracked as counters, with the count that completes each. */
    public static final Map<String, Integer> ACHIEVEMENTS = new LinkedHashMap<>();

    static {
        String ns = "holidayseasons:";
        ITEMS.put("decorations", List.of(ns + "candy_bowl", ns + "doorbell", ns + "holiday_hearth", ns + "tombstone", ns + "hanging_ghost",
                ns + "carved_pumpkin_grin", ns + "jack_o_lantern_scary", ns + "christmas_tree", ns + "wreath", ns + "string_lights",
                ns + "present", ns + "easter_basket", ns + "wall_calendar"));
        ITEMS.put("costumes", List.of(ns + "costume_pumpkin", ns + "costume_ghost", ns + "costume_skeleton", ns + "costume_witch",
                ns + "costume_vampire", ns + "costume_scarecrow", ns + "costume_bat", ns + "costume_black_cat", ns + "santa_hat",
                ns + "elf_hat", ns + "antlers", ns + "winter_beanie"));
        ITEMS.put("treats", List.of(ns + "candy_corn", ns + "caramel_apple", ns + "chocolate_bar", ns + "lollipop", ns + "gummy_bat",
                ns + "chocolate_box", ns + "harvest_pie", ns + "roast_turkey"));
        ITEMS.put("rare", List.of(ns + "golden_egg", ns + "snow_globe", PEDDLERS_LANTERN, ns + "ectoplasm", ns + "holiday_token"));
        ACHIEVEMENTS.put("eggs_found", 10);
        ACHIEVEMENTS.put("candy_given", 10);
        ACHIEVEMENTS.put("presents_given", 5);
        ACHIEVEMENTS.put("objectives_completed", 1);
        ACHIEVEMENTS.put("parties_hosted", 1);
        ACHIEVEMENTS.put("guests_welcomed", 10);
    }

    private Collectibles() {
    }

    public static int itemCount() {
        return ITEMS.values().stream().mapToInt(List::size).sum();
    }

    public static boolean isCollectible(String id) {
        for (List<String> l : ITEMS.values()) if (l.contains(id)) return true;
        return false;
    }

    /** Completion in percent: collectibles, wildlife, encounters and finished achievements weigh equally per entry. */
    public static int completion(int items, int wildlife, int encounters, int achievements) {
        int total = itemCount() + WILDLIFE.size() + ENCOUNTERS.size() + ACHIEVEMENTS.size();
        int have = Math.min(items, itemCount()) + Math.min(wildlife, WILDLIFE.size()) + Math.min(encounters, ENCOUNTERS.size())
                + Math.min(achievements, ACHIEVEMENTS.size());
        return have * 100 / total;
    }
}
