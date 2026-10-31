package com.holidayseasons.event;

import java.util.List;
import java.util.Map;

/**
 * Village-wide objectives: during a holiday each celebrating village asks for a batch of something festive, brought
 * to its bell. Pure rules (no game classes) so they can be unit-tested.
 */
public final class ObjectiveRules {
    /** {@code item}: an item id, or {@code #tag} for an item tag. */
    public record Objective(String item, int needed) {
        public boolean isTag() {
            return item.startsWith("#");
        }
    }

    private static final Map<String, List<Objective>> BY_HOLIDAY = Map.of(
            "halloween", List.of(new Objective("#holidayseasons:candy", 32), new Objective("minecraft:pumpkin", 24), new Objective("minecraft:torch", 32)),
            "christmas", List.of(new Objective("minecraft:spruce_log", 32), new Objective("minecraft:cookie", 32), new Objective("holidayseasons:ornament", 16)),
            "easter", List.of(new Objective("#holidayseasons:easter_eggs", 12), new Objective("#minecraft:small_flowers", 24), new Objective("minecraft:wheat", 32)),
            "valentines", List.of(new Objective("#minecraft:small_flowers", 24), new Objective("minecraft:cocoa_beans", 16), new Objective("minecraft:sugar", 24)),
            "thanksgiving", List.of(new Objective("minecraft:wheat", 48), new Objective("minecraft:carrot", 32), new Objective("minecraft:potato", 32),
                    new Objective("minecraft:pumpkin", 16)),
            "new_years", List.of(new Objective("minecraft:gunpowder", 16), new Objective("minecraft:paper", 24), new Objective("minecraft:lantern", 8)),
            "independence_day", List.of(new Objective("minecraft:gunpowder", 16), new Objective("minecraft:red_wool", 16), new Objective("minecraft:blue_wool", 16)));
    private static final List<Objective> FALLBACK = List.of(new Objective("minecraft:bread", 24));

    private ObjectiveRules() {
    }

    /** The objective a village gets for a holiday occurrence: stable for that village and year, varying between them. */
    public static Objective choose(String holidayId, String occurrenceKey, String villageKey) {
        List<Objective> options = BY_HOLIDAY.getOrDefault(holidayId.substring(holidayId.indexOf(':') + 1), FALLBACK);
        int h = (occurrenceKey + "|" + villageKey).hashCode();
        return options.get(Math.floorMod(h ^ (h >>> 16), options.size()));
    }

    /** How many of {@code offered} items are taken: never more than still needed. */
    public static int take(int progress, int needed, int offered) {
        return Math.max(0, Math.min(offered, needed - progress));
    }
}
