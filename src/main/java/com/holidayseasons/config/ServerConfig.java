package com.holidayseasons.config;

import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSettings;
import com.holidayseasons.calendar.SeasonSyncPolicy;
import com.holidayseasons.calendar.TimeJumpPolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-authoritative settings, stored in {@code config/holidayseasons-server.toml}
 * and synchronised to clients by NeoForge. Only operators can change them at runtime.
 */
public final class ServerConfig {
    // Must be initialised before the spec is built in the static block below.
    /** Built-in holidays with config sections, and whether each is enabled by default. */
    public static final Map<String, Boolean> BUILT_IN_HOLIDAYS = Map.of(
            "halloween", true, "christmas", true, "easter", true, "new_years", true,
            "valentines", true, "thanksgiving", true, "independence_day", false);
    public static final List<String> BUILT_IN_ORDER = List.of(
            "new_years", "valentines", "easter", "independence_day", "halloween", "thanksgiving", "christmas");

    public static final ModConfigSpec SPEC;
    public static final ServerConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ServerConfig(builder);
        SPEC = builder.build();
    }

    // calendar
    public final ModConfigSpec.EnumValue<CalendarMode> calendarMode;
    public final ModConfigSpec.EnumValue<TimeJumpPolicy> timeJumpPolicy;
    public final ModConfigSpec.ConfigValue<List<? extends Integer>> customMonthLengths;
    public final ModConfigSpec.IntValue customLeapInterval;
    public final ModConfigSpec.IntValue customLeapMonth;
    public final ModConfigSpec.ConfigValue<String> customStartDate;
    public final ModConfigSpec.IntValue referenceYearBase;
    public final ModConfigSpec.IntValue fallbackDaysPerMonth;
    public final ModConfigSpec.ConfigValue<String> realWorldTimezone;
    // seasons
    public final ModConfigSpec.EnumValue<SeasonSyncPolicy> seasonSyncPolicy;
    public final ModConfigSpec.BooleanValue warnOnSeasonMismatch;
    // sync / performance
    public final ModConfigSpec.IntValue clientSyncIntervalTicks;
    // holidays
    public final ModConfigSpec.BooleanValue announceHolidays;
    public final ModConfigSpec.ConfigValue<List<? extends String>> disabledHolidays;
    public final ModConfigSpec.IntValue historyYears;
    public final Map<String, HolidayOverrides> holidays = new LinkedHashMap<>();
    // villages
    public final ModConfigSpec.BooleanValue villageCelebrations;
    public final ModConfigSpec.BooleanValue festivalSchedules;
    public final ModConfigSpec.IntValue villagerBudgetPerTick;
    public final ModConfigSpec.IntValue taskCooldownTicks;
    public final ModConfigSpec.BooleanValue villagerDecorations;
    public final ModConfigSpec.IntValue maxDecorationsPerVillage;
    public final ModConfigSpec.IntValue decorationRadius;
    public final ModConfigSpec.IntValue decorationLedgerBudget;
    public final ModConfigSpec.BooleanValue reconcileUnloadedVillages;


    /** Per-holiday overrides for a built-in holiday. Negative numbers / empty strings mean "use the datapack value". */
    public static final class HolidayOverrides {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.ConfigValue<String> dateRule;
        public final ModConfigSpec.IntValue preparationDays;
        public final ModConfigSpec.IntValue durationDays;
        public final ModConfigSpec.IntValue cleanupDays;
        public final ModConfigSpec.DoubleValue intensity;

        HolidayOverrides(ModConfigSpec.Builder b, String id, boolean enabledByDefault) {
            String t = "holidayseasons.configuration.holiday.";
            b.push(id);
            enabled = b.translation(t + "enabled").define("enabled", enabledByDefault);
            dateRule = b.comment("Date rule override, e.g. fixed:10-31, nth_weekday:11:thursday:4, easter:0, native:4-3,",
                            "season:mid_spring:0.5. Empty = use the holiday's datapack definition.")
                    .translation(t + "dateRule").define("dateRule", "");
            preparationDays = b.comment("Days of preparation before the holiday. -1 = datapack default.")
                    .translation(t + "preparationDays").defineInRange("preparationDays", -1, -1, 60);
            durationDays = b.comment("Celebration length in days. -1 = datapack default.")
                    .translation(t + "durationDays").defineInRange("durationDays", -1, -1, 60);
            cleanupDays = b.comment("Days over which temporary decorations disappear afterwards. -1 = datapack default.")
                    .translation(t + "cleanupDays").defineInRange("cleanupDays", -1, -1, 60);
            intensity = b.comment("Celebration intensity: scales how many villagers take part and how much they decorate.")
                    .translation(t + "intensity").defineInRange("intensity", 1.0, 0.0, 2.0);
            b.pop();
        }
    }

    private ServerConfig(ModConfigSpec.Builder b) {
        b.comment("Calendar settings. The server is authoritative; clients only display what it sends.").push("calendar");
        calendarMode = b.comment(
                        "SEASON_SYNCED: Minecraft-time; the Serene Seasons year is split into 12 months (one per sub-season).",
                        "CUSTOM: Minecraft-time; EverFest's own year using customMonthLengths (one Minecraft day = one calendar day).",
                        "REAL_WORLD: the server's real date in realWorldTimezone. Admins can override it with /everfest date.")
                .translation("holidayseasons.configuration.calendarMode")
                .defineEnum("mode", CalendarMode.SEASON_SYNCED);
        timeJumpPolicy = b.comment(
                        "CUSTOM mode: what happens when the world clock moves backwards (e.g. /time set day).",
                        "FORWARD_TO_NEXT_TIME_OF_DAY never rewinds the calendar, KEEP_DATE only changes the hour, REWIND moves the date back.",
                        "Completed holidays are never celebrated twice in the same year regardless of this setting.")
                .translation("holidayseasons.configuration.timeJumpPolicy")
                .defineEnum("timeJumpPolicy", TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY);
        customMonthLengths = b.comment("CUSTOM mode: length in days of each of the 12 months.")
                .translation("holidayseasons.configuration.customMonthLengths")
                .defineList("customMonthLengths", CalendarSettings.toList(CalendarSettings.GREGORIAN_LENGTHS),
                        () -> 30, o -> o instanceof Integer i && i >= 1 && i <= 400);
        customLeapInterval = b.comment("CUSTOM mode: every N-th year gains a leap day. 0 disables leap years.")
                .translation("holidayseasons.configuration.customLeapInterval")
                .defineInRange("customLeapInterval", 4, 0, 1000);
        customLeapMonth = b.comment("CUSTOM mode: month (1-12) that receives the leap day.")
                .translation("holidayseasons.configuration.customLeapMonth")
                .defineInRange("customLeapMonth", 2, 1, 12);
        customStartDate = b.comment("CUSTOM mode: date (YYYY-MM-DD) of a brand-new world's first day.")
                .translation("holidayseasons.configuration.customStartDate")
                .define("customStartDate", "0001-03-01");
        referenceYearBase = b.comment("Minecraft-time modes: rule-based holidays (Easter, Thanksgiving) for year N use the",
                        "Gregorian rules of year (referenceYearBase + N).")
                .translation("holidayseasons.configuration.referenceYearBase")
                .defineInRange("referenceYearBase", 2000, 1, 100000);
        fallbackDaysPerMonth = b.comment("SEASON_SYNCED mode without Serene Seasons installed: days per month.")
                .translation("holidayseasons.configuration.fallbackDaysPerMonth")
                .defineInRange("fallbackDaysPerMonth", 8, 1, 400);
        realWorldTimezone = b.comment("REAL_WORLD mode: IANA timezone such as America/New_York, or 'system'.")
                .translation("holidayseasons.configuration.realWorldTimezone")
                .define("realWorldTimezone", "system");
        b.pop();

        b.comment("Serene Seasons integration.").push("seasons");
        seasonSyncPolicy = b.comment(
                        "When the calendar date and the in-game season disagree:",
                        "FOLLOW_DATE: holidays follow their dates.",
                        "FOLLOW_SEASON: holidays are scheduled on the Serene Seasons cycle (needs Serene Seasons).",
                        "REQUIRE_MATCHING_SEASON: holidays follow their dates but are skipped if the season does not match.")
                .translation("holidayseasons.configuration.seasonSyncPolicy")
                .defineEnum("syncPolicy", SeasonSyncPolicy.FOLLOW_DATE);
        warnOnSeasonMismatch = b.comment("Log a warning at startup if a CUSTOM calendar year length differs from the Serene Seasons cycle.")
                .translation("holidayseasons.configuration.warnOnSeasonMismatch")
                .define("warnOnSeasonMismatch", true);
        b.pop();

        b.comment("Networking and performance budgets.").push("performance");
        clientSyncIntervalTicks = b.comment("Ticks between periodic calendar syncs to clients (changes are always sent immediately).")
                .translation("holidayseasons.configuration.clientSyncIntervalTicks")
                .defineInRange("clientSyncIntervalTicks", 200, 20, 6000);
        b.pop();

        b.comment("Holiday schedule. Built-in holidays can be tuned here; datapacks can add more under",
                "data/<namespace>/everfest/holiday/<name>.json.").push("holidays");
        announceHolidays = b.comment("Announce holiday preparation and start in chat.")
                .translation("holidayseasons.configuration.announceHolidays")
                .define("announceHolidays", true);
        disabledHolidays = b.comment("Ids of datapack holidays to disable, e.g. [\"mypack:harvest_moon\"].")
                .translation("holidayseasons.configuration.disabledHolidays")
                .defineListAllowEmpty("disabledHolidays", List.of(), () -> "namespace:holiday", o -> o instanceof String);
        historyYears = b.comment("Years of holiday history to keep (prevents re-celebrating after large time rewinds).")
                .translation("holidayseasons.configuration.historyYears")
                .defineInRange("historyYears", 3, 1, 50);
        for (String id : BUILT_IN_ORDER) {
            holidays.put(id, new HolidayOverrides(b, id, BUILT_IN_HOLIDAYS.get(id)));
        }
        b.pop();

        b.comment("Autonomous village celebrations.").push("villages");
        String v = "holidayseasons.configuration.villages.";
        villageCelebrations = b.comment("Villagers prepare for and celebrate holidays on their own.")
                .translation(v + "enabled").define("enabled", true);
        festivalSchedules = b.comment("On celebration days villagers work less and spend more time at the festival square.",
                        "Only replaces vanilla's default villager schedules, never schedules set by other mods; reverts afterwards.")
                .translation(v + "festivalSchedules").define("festivalSchedules", true);
        villagerBudgetPerTick = b.comment("Maximum villagers EverFest examines per server tick (performance budget).")
                .translation(v + "villagerBudgetPerTick").defineInRange("villagerBudgetPerTick", 6, 1, 100);
        taskCooldownTicks = b.comment("Minimum ticks between two holiday activities of the same villager.")
                .translation(v + "taskCooldownTicks").defineInRange("taskCooldownTicks", 300, 20, 24000);
        villagerDecorations = b.comment("Villagers put up temporary decorations (display entities and invisible light).",
                        "Decorations never replace blocks and disappear over the holiday's cleanup days.")
                .translation(v + "decorations").define("decorations", true);
        maxDecorationsPerVillage = b.comment("Decorations per village at full intensity.")
                .translation(v + "maxDecorationsPerVillage").defineInRange("maxDecorationsPerVillage", 18, 0, 200);
        decorationRadius = b.comment("How far from the festival square villagers decorate.")
                .translation(v + "decorationRadius").defineInRange("decorationRadius", 20, 4, 64);
        decorationLedgerBudget = b.comment("Decoration ledger entries checked per tick for cleanup.")
                .translation(v + "decorationLedgerBudget").defineInRange("decorationLedgerBudget", 16, 1, 1000);
        reconcileUnloadedVillages = b.comment("When a village loads after being unloaded during preparations, put up the decorations",
                        "its villagers would have made meanwhile.")
                .translation(v + "reconcileUnloadedVillages").define("reconcileUnloadedVillages", true);
        b.pop();
    }

    public CalendarSettings calendarSettings() {
        if (!SPEC.isLoaded()) return CalendarSettings.defaults();
        return CalendarSettings.sanitize(calendarMode.get(), timeJumpPolicy.get(), customMonthLengths.get(),
                customLeapInterval.get(), customLeapMonth.get(), customStartDate.get(), referenceYearBase.get(),
                fallbackDaysPerMonth.get(), realWorldTimezone.get(), seasonSyncPolicy.get());
    }
}
