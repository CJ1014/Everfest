package com.holidayseasons.holiday;

import com.holidayseasons.calendar.Season;
import com.holidayseasons.holiday.rule.DateRule;
import com.holidayseasons.holiday.rule.DateRules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * A holiday as defined by a datapack JSON file at {@code data/<namespace>/everfest/holiday/<name>.json}.
 * The id is {@code <namespace>:<name>}. Server config can override date, durations and the enabled flag of the
 * built-in holidays; datapacks can add new holidays or replace built-in ones.
 *
 * @param date             rule for the first celebration day
 * @param durationDays     celebration length in days (>= 1)
 * @param preparationDays  days of preparation before the first celebration day
 * @param cleanupDays      days after the celebration during which temporary decorations fade away
 * @param enabled          default enabled state
 * @param seasons          seasons the holiday normally falls in (used by REQUIRE_MATCHING_SEASON)
 * @param color            ARGB accent colour for UI
 * @param behaviors        village celebration behaviour ids (e.g. decorate, gather, gift_exchange, trick_or_treat)
 * @param decorations      decoration theme ids placed by villagers
 * @param activities       player activity ids
 * @param ambience         music and atmosphere ids
 * @param rewards          rewards granted for participation
 * @param specialEvents    optional rare events with a daily chance while active
 * @param settings         free-form numeric gameplay tunables
 */
public record HolidayDefinition(
        DateRule date,
        int durationDays,
        int preparationDays,
        int cleanupDays,
        boolean enabled,
        List<Season> seasons,
        int color,
        List<String> behaviors,
        List<String> decorations,
        List<String> activities,
        Ambience ambience,
        Rewards rewards,
        List<SpecialEvent> specialEvents,
        Map<String, Double> settings
) {
    public static final Codec<Season> SEASON_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(Season.byId(s));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, Season::id);

    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            String hex = s.startsWith("#") ? s.substring(1) : s;
            return DataResult.success(0xFF000000 | Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid colour " + s);
        }
    }, c -> String.format("#%06X", c & 0xFFFFFF));

    public record Ambience(String music, String ambientSound, String fog, String particles) {
        public static final Ambience NONE = new Ambience("", "", "", "");
        public static final Codec<Ambience> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("music", "").forGetter(Ambience::music),
                Codec.STRING.optionalFieldOf("ambient_sound", "").forGetter(Ambience::ambientSound),
                Codec.STRING.optionalFieldOf("fog", "").forGetter(Ambience::fog),
                Codec.STRING.optionalFieldOf("particles", "").forGetter(Ambience::particles)
        ).apply(i, Ambience::new));
    }

    public record Rewards(int reputation, int tokens, List<String> items) {
        public static final Rewards NONE = new Rewards(0, 0, List.of());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1000).optionalFieldOf("reputation", 0).forGetter(Rewards::reputation),
                Codec.intRange(0, 1000).optionalFieldOf("tokens", 0).forGetter(Rewards::tokens),
                Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(Rewards::items)
        ).apply(i, Rewards::new));
    }

    public record SpecialEvent(String id, double dailyChance) {
        public static final Codec<SpecialEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(SpecialEvent::id),
                Codec.doubleRange(0, 1).optionalFieldOf("daily_chance", 0.1).forGetter(SpecialEvent::dailyChance)
        ).apply(i, SpecialEvent::new));
    }

    public static final Codec<HolidayDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            DateRules.CODEC.fieldOf("date").forGetter(HolidayDefinition::date),
            Codec.intRange(1, 60).optionalFieldOf("duration_days", 1).forGetter(HolidayDefinition::durationDays),
            Codec.intRange(0, 60).optionalFieldOf("preparation_days", 4).forGetter(HolidayDefinition::preparationDays),
            Codec.intRange(0, 60).optionalFieldOf("cleanup_days", 3).forGetter(HolidayDefinition::cleanupDays),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(HolidayDefinition::enabled),
            SEASON_CODEC.listOf().optionalFieldOf("seasons", List.of()).forGetter(HolidayDefinition::seasons),
            COLOR_CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(HolidayDefinition::color),
            Codec.STRING.listOf().optionalFieldOf("behaviors", List.of()).forGetter(HolidayDefinition::behaviors),
            Codec.STRING.listOf().optionalFieldOf("decorations", List.of()).forGetter(HolidayDefinition::decorations),
            Codec.STRING.listOf().optionalFieldOf("activities", List.of()).forGetter(HolidayDefinition::activities),
            Ambience.CODEC.optionalFieldOf("ambience", Ambience.NONE).forGetter(HolidayDefinition::ambience),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(HolidayDefinition::rewards),
            SpecialEvent.CODEC.listOf().optionalFieldOf("special_events", List.of()).forGetter(HolidayDefinition::specialEvents),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("settings", Map.of()).forGetter(HolidayDefinition::settings)
    ).apply(i, HolidayDefinition::new));

    public boolean hasBehavior(String behavior) {
        return behaviors.contains(behavior);
    }

    public double setting(String key, double fallback) {
        return settings.getOrDefault(key, fallback);
    }
}
