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
    // homes
    public final ModConfigSpec.IntValue maxHomesPerPlayer;
    public final ModConfigSpec.IntValue minHomeDistance;
    public final ModConfigSpec.IntValue homeProtectionRadius;
    public final ModConfigSpec.BooleanValue protectHearths;
    // halloween
    public final ModConfigSpec.IntValue candyPerVisitorPerDay;
    public final ModConfigSpec.BooleanValue trickOrTreating;
    public final ModConfigSpec.IntValue maxTrickOrTreatGroups;
    public final ModConfigSpec.IntValue trickOrTreatRadius;
    public final ModConfigSpec.IntValue maxVisitsPerHouse;
    public final ModConfigSpec.BooleanValue villagerCostumes;
    public final ModConfigSpec.BooleanValue harmlessTricks;
    public final ModConfigSpec.BooleanValue halloweenCreatures;
    public final ModConfigSpec.IntValue creaturesPerPlayer;
    public final ModConfigSpec.DoubleValue creatureSpawnChance;
    // wildlife
    public final ModConfigSpec.BooleanValue reindeer;
    public final ModConfigSpec.BooleanValue springBunnies;
    public final ModConfigSpec.BooleanValue wildTurkeys;
    public final ModConfigSpec.IntValue seasonalSpawnIntervalTicks;
    public final ModConfigSpec.DoubleValue seasonalSpawnChance;
    public final ModConfigSpec.IntValue maxSeasonalAnimalsNearPlayer;
    // economy and rare events
    public final ModConfigSpec.BooleanValue holidayTokens;
    public final ModConfigSpec.BooleanValue holidayMerchants;
    public final ModConfigSpec.DoubleValue merchantVisitChance;
    public final ModConfigSpec.IntValue merchantStayTicks;
    public final ModConfigSpec.BooleanValue midnightPeddler;
    public final ModConfigSpec.DoubleValue midnightPeddlerChance;
    public final ModConfigSpec.BooleanValue villageObjectives;
    public final ModConfigSpec.BooleanValue graveyardHaunting;
    public final ModConfigSpec.BooleanValue festivalMarkets;
    public final ModConfigSpec.BooleanValue hearthParties;
    public final ModConfigSpec.IntValue maxPartyGuests;
    public final ModConfigSpec.IntValue partyInviteRadius;
    public final ModConfigSpec.IntValue partyDurationTicks;
    // worldgen
    public final ModConfigSpec.BooleanValue hauntedCottages;
    public final ModConfigSpec.BooleanValue forgottenGraveyards;
    public final ModConfigSpec.BooleanValue seasonalCabins;


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

        b.comment("Player homes registered with a Holiday Hearth.").push("homes");
        String h = "holidayseasons.configuration.homes.";
        maxHomesPerPlayer = b.translation(h + "maxHomesPerPlayer").defineInRange("maxHomesPerPlayer", 2, 0, 32);
        minHomeDistance = b.comment("Minimum distance between hearths of different players.")
                .translation(h + "minHomeDistance").defineInRange("minHomeDistance", 16, 0, 256);
        homeProtectionRadius = b.comment("Villagers never decorate within this radius of a hearth unless its owner allows it.")
                .translation(h + "homeProtectionRadius").defineInRange("homeProtectionRadius", 12, 0, 64);
        protectHearths = b.comment("Only the owner (or an operator) can break a Holiday Hearth.")
                .translation(h + "protectHearths").define("protectHearths", true);
        b.pop();

        b.comment("Halloween gameplay.").push("halloween");
        String hw = "holidayseasons.configuration.halloween.";
        trickOrTreating = b.comment("Village children go trick-or-treating on Halloween.")
                .translation(hw + "trickOrTreating").define("trickOrTreating", true);
        maxTrickOrTreatGroups = b.comment("Maximum trick-or-treat groups out at once per village.")
                .translation(hw + "maxTrickOrTreatGroups").defineInRange("maxTrickOrTreatGroups", 3, 0, 16);
        trickOrTreatRadius = b.comment("How far from their village children walk to knock on doors (blocks).")
                .translation(hw + "trickOrTreatRadius").defineInRange("trickOrTreatRadius", 48, 8, 160);
        maxVisitsPerHouse = b.comment("How many groups may knock on the same door per Halloween.")
                .translation(hw + "maxVisitsPerHouse").defineInRange("maxVisitsPerHouse", 2, 1, 20);
        candyPerVisitorPerDay = b.comment("How many candies each player may take from someone else's candy bowl per day.")
                .translation(hw + "candyPerVisitorPerDay").defineInRange("candyPerVisitorPerDay", 1, 0, 64);
        villagerCostumes = b.comment("Villagers wear costumes on Halloween (temporary, removed afterwards, never dropped).")
                .translation(hw + "villagerCostumes").define("villagerCostumes", true);
        harmlessTricks = b.comment("Children leave a harmless temporary trick (cobwebs, a pumpkin) when nobody answers.")
                .translation(hw + "harmlessTricks").define("harmlessTricks", true);
        halloweenCreatures = b.comment("Ghosts and gourdlings appear on Halloween nights (only then).")
                .translation(hw + "creatures").define("creatures", true);
        creaturesPerPlayer = b.comment("Maximum Halloween creatures near each player.")
                .translation(hw + "creaturesPerPlayer").defineInRange("creaturesPerPlayer", 4, 0, 32);
        creatureSpawnChance = b.comment("Chance per player per second to try spawning a Halloween creature at night.")
                .translation(hw + "creatureSpawnChance").defineInRange("creatureSpawnChance", 0.08, 0.0, 1.0);
        b.pop();

        b.comment("Seasonal wildlife. Reindeer spawn naturally in snowy biomes (weights: data/holidayseasons/neoforge/biome_modifier).",
                "Spring bunnies and wild turkeys visit in their season only, a few at a time near players.").push("wildlife");
        String wl = "holidayseasons.configuration.wildlife.";
        reindeer = b.comment("Reindeer spawn in snowy biomes.").translation(wl + "reindeer").define("reindeer", true);
        springBunnies = b.comment("Spring bunnies appear in spring in grassy and flowery biomes.")
                .translation(wl + "springBunnies").define("springBunnies", true);
        wildTurkeys = b.comment("Wild turkeys appear in autumn in woods and meadows.")
                .translation(wl + "wildTurkeys").define("wildTurkeys", true);
        seasonalSpawnIntervalTicks = b.comment("How often (ticks) each player is considered for a seasonal animal visit.")
                .translation(wl + "seasonalSpawnIntervalTicks").defineInRange("seasonalSpawnIntervalTicks", 600, 100, 24000);
        seasonalSpawnChance = b.comment("Chance per check that a small group of in-season animals arrives near a player.")
                .translation(wl + "seasonalSpawnChance").defineInRange("seasonalSpawnChance", 0.3, 0.0, 1.0);
        maxSeasonalAnimalsNearPlayer = b.comment("No more arrive while this many of that species are already within 64 blocks.")
                .translation(wl + "maxSeasonalAnimalsNearPlayer").defineInRange("maxSeasonalAnimalsNearPlayer", 5, 0, 32);
        b.pop();

        b.comment("Holiday landmarks generated in new chunks. Turning one off stops it appearing in chunks generated from",
                "now on; existing ones stay. Where and how often they appear is data-driven (data/holidayseasons/worldgen).").push("worldgen");
        String wg = "holidayseasons.configuration.worldgen.";
        hauntedCottages = b.comment("Abandoned haunted cottages in dark forests, swamps and taigas.")
                .translation(wg + "hauntedCottages").define("hauntedCottages", true);
        forgottenGraveyards = b.comment("Forgotten graveyards in plains and forests (haunted on Halloween nights).")
                .translation(wg + "forgottenGraveyards").define("forgottenGraveyards", true);
        seasonalCabins = b.comment("Snowy seasonal cabins with a hearth a player can claim as a home.")
                .translation(wg + "seasonalCabins").define("seasonalCabins", true);
        b.pop();

        b.comment("Emeralds stay the main currency. Holiday merchants sell a few limited-use goods and never buy anything back.").push("economy");
        String ec = "holidayseasons.configuration.economy.";
        holidayTokens = b.comment("Festival tokens: earned through community activities, spent on token-only collectibles.")
                .translation(ec + "holidayTokens").define("holidayTokens", false);
        holidayMerchants = b.comment("Holiday merchants visit celebrating villages while a player is nearby.")
                .translation(ec + "holidayMerchants").define("holidayMerchants", true);
        merchantVisitChance = b.comment("Chance that a merchant visits a celebrating village on a given holiday day.")
                .translation(ec + "merchantVisitChance").defineInRange("merchantVisitChance", 0.6, 0.0, 1.0);
        merchantStayTicks = b.comment("How long a merchant stays (ticks) unless the holiday ends first.")
                .translation(ec + "merchantStayTicks").defineInRange("merchantStayTicks", 12000, 1200, 48000);
        b.pop();

        b.comment("Rare events.").push("events");
        String ev = "holidayseasons.configuration.events.";
        midnightPeddler = b.comment("The Midnight Peddler may visit players outdoors on Halloween night (once per player per Halloween).")
                .translation(ev + "midnightPeddler").define("midnightPeddler", true);
        midnightPeddlerChance = b.comment("Chance per Halloween night that the peddler visits a player.")
                .translation(ev + "midnightPeddlerChance").defineInRange("midnightPeddlerChance", 0.35, 0.0, 1.0);
        villageObjectives = b.comment("Villages ask for a batch of festive goods at their bell during each holiday; contributors are rewarded.")
                .translation(ev + "villageObjectives").define("villageObjectives", true);
        graveyardHaunting = b.comment("On Halloween night, ghosts rise from forgotten graveyards when a player walks in (once per night).")
                .translation(ev + "graveyardHaunting").define("graveyardHaunting", true);
        festivalMarkets = b.comment("A visiting merchant sets up a temporary market stall (display entities only, removed after the holiday).")
                .translation(ev + "festivalMarkets").define("festivalMarkets", true);
        b.pop();

        b.comment("Holiday parties hosted from a Holiday Hearth.").push("parties");
        String pa = "holidayseasons.configuration.parties.";
        hearthParties = b.comment("Players can host a party at their home while a holiday is being celebrated.")
                .translation(pa + "hearthParties").define("hearthParties", true);
        maxPartyGuests = b.comment("Most villagers invited to one party.")
                .translation(pa + "maxPartyGuests").defineInRange("maxPartyGuests", 6, 0, 16);
        partyInviteRadius = b.comment("Villagers within this many blocks of the home are invited (they walk; nobody is teleported).")
                .translation(pa + "partyInviteRadius").defineInRange("partyInviteRadius", 48, 8, 96);
        partyDurationTicks = b.comment("How long a party lasts (ticks).")
                .translation(pa + "partyDurationTicks").defineInRange("partyDurationTicks", 4800, 1200, 24000);
        b.pop();
    }

    public CalendarSettings calendarSettings() {
        if (!SPEC.isLoaded()) return CalendarSettings.defaults();
        return CalendarSettings.sanitize(calendarMode.get(), timeJumpPolicy.get(), customMonthLengths.get(),
                customLeapInterval.get(), customLeapMonth.get(), customStartDate.get(), referenceYearBase.get(),
                fallbackDaysPerMonth.get(), realWorldTimezone.get(), seasonSyncPolicy.get());
    }
}
