package com.holidayseasons.config;

import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSettings;
import com.holidayseasons.calendar.SeasonSyncPolicy;
import com.holidayseasons.calendar.TimeJumpPolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server-authoritative settings, stored in {@code config/holidayseasons-server.toml}
 * and synchronised to clients by NeoForge. Only operators can change them at runtime.
 */
public final class ServerConfig {
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
    }

    public CalendarSettings calendarSettings() {
        if (!SPEC.isLoaded()) return CalendarSettings.defaults();
        return CalendarSettings.sanitize(calendarMode.get(), timeJumpPolicy.get(), customMonthLengths.get(),
                customLeapInterval.get(), customLeapMonth.get(), customStartDate.get(), referenceYearBase.get(),
                fallbackDaysPerMonth.get(), realWorldTimezone.get(), seasonSyncPolicy.get());
    }
}
