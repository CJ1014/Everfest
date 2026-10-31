package com.holidayseasons.entity;

import java.util.Set;
import java.util.UUID;

/**
 * Which vanilla animals dress up for which holiday. Purely cosmetic and computed the same way on every client from the
 * animal's UUID and the holidays currently preparing or celebrating, so nothing needs to be stored or synced, and the
 * same sheep always wears its scarf every Christmas.
 */
public final class FestiveOutfits {
    public enum Outfit {
        SCARF("sheep_scarf"),
        BELL_COLLAR("cow_bell_collar"),
        BANDANA("wolf_bandana"),
        FLOWER_CROWN("rabbit_flower_crown");

        public final String texture;

        Outfit(String texture) {
            this.texture = texture;
        }
    }

    private record Rule(String entityType, String holiday, Outfit outfit, int oneIn, long salt) {
    }

    private static final String NS = "holidayseasons:";
    private static final Rule[] RULES = {
            new Rule("minecraft:sheep", NS + "christmas", Outfit.SCARF, 3, 0x5CA4FL),
            new Rule("minecraft:cow", NS + "christmas", Outfit.BELL_COLLAR, 3, 0xBE11L),
            new Rule("minecraft:wolf", NS + "halloween", Outfit.BANDANA, 2, 0xBA4DL),
            new Rule("minecraft:rabbit", NS + "easter", Outfit.FLOWER_CROWN, 2, 0xF10EL),
    };

    private FestiveOutfits() {
    }

    /** The outfit this animal wears right now, or null. {@code festiveHolidays}: ids preparing or celebrating. */
    public static Outfit outfitFor(String entityType, UUID uuid, Set<String> festiveHolidays) {
        if (festiveHolidays.isEmpty()) return null;
        for (Rule r : RULES) {
            if (r.entityType.equals(entityType) && festiveHolidays.contains(r.holiday) && bucket(uuid, r.salt, r.oneIn) == 0) {
                return r.outfit;
            }
        }
        return null;
    }

    static int bucket(UUID uuid, long salt, int buckets) {
        long h = uuid.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ uuid.getLeastSignificantBits() ^ salt;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) Math.floorMod(h, (long) buckets);
    }
}
