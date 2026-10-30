package com.holidayseasons.holiday;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.holiday.rule.DateRule;
import com.holidayseasons.holiday.rule.DateRules;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads holiday definitions from datapacks ({@code data/<ns>/everfest/holiday/*.json}) and combines them with the
 * server config into {@link ScheduledHoliday}s. Invalid files are logged and skipped, never fatal.
 */
public final class HolidayRegistry extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "everfest/holiday";
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<String, HolidayDefinition> definitions = Map.of();
    private static Runnable reloadCallback = () -> {
    };

    public HolidayRegistry() {
        super(GSON, DIRECTORY);
    }

    public static void onReload(Runnable callback) {
        reloadCallback = callback;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, HolidayDefinition> loaded = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> HolidayDefinition.CODEC
                .parse(JsonOps.INSTANCE, e.getValue())
                .resultOrPartial(err -> EverFest.LOGGER.error("EverFest: invalid holiday {}: {}", e.getKey(), err))
                .ifPresent(def -> loaded.put(e.getKey().toString(), def)));
        definitions = Collections.unmodifiableMap(loaded);
        EverFest.LOGGER.info("EverFest: loaded {} holidays: {}", loaded.size(), loaded.keySet());
        reloadCallback.run();
    }

    public static Map<String, HolidayDefinition> definitions() {
        return definitions;
    }

    /** Built-in holiday id for a config section name. */
    public static String builtInId(String name) {
        return EverFest.MOD_ID + ":" + name;
    }

    /** Applies server config overrides and returns the holidays to schedule, sorted by id. */
    public static List<ScheduledHoliday> scheduled() {
        List<ScheduledHoliday> out = new ArrayList<>();
        boolean configLoaded = ServerConfig.SPEC.isLoaded();
        List<? extends String> disabled = configLoaded ? ServerConfig.INSTANCE.disabledHolidays.get() : List.of();
        for (Map.Entry<String, HolidayDefinition> e : definitions.entrySet()) {
            String id = e.getKey();
            HolidayDefinition def = e.getValue();
            DateRule rule = def.date();
            int prep = def.preparationDays();
            int dur = def.durationDays();
            int clean = def.cleanupDays();
            boolean enabled = def.enabled() && !disabled.contains(id);
            ServerConfig.HolidayOverrides o = null;
            if (configLoaded && id.startsWith(EverFest.MOD_ID + ":")) {
                o = ServerConfig.INSTANCE.holidays.get(id.substring(EverFest.MOD_ID.length() + 1));
            }
            if (o != null) {
                enabled = o.enabled.get() && !disabled.contains(id);
                String override = o.dateRule.get();
                if (!override.isBlank()) {
                    var parsed = DateRules.tryParse(override);
                    if (parsed.result().isPresent()) rule = parsed.result().get();
                    else EverFest.LOGGER.warn("EverFest: ignoring invalid dateRule '{}' for {}", override, id);
                }
                if (o.preparationDays.get() >= 0) prep = o.preparationDays.get();
                if (o.durationDays.get() >= 1) dur = o.durationDays.get();
                if (o.cleanupDays.get() >= 0) clean = o.cleanupDays.get();
            }
            List<Season> seasons = def.seasons();
            out.add(new ScheduledHoliday(id, rule, prep, dur, clean, enabled, seasons, def.color(), def));
        }
        out.sort(Comparator.comparing(ScheduledHoliday::id));
        return out;
    }

    /** Celebration intensity (0..2) from config; 1 for datapack holidays. */
    public static double intensity(String id) {
        if (!ServerConfig.SPEC.isLoaded() || !id.startsWith(EverFest.MOD_ID + ":")) return 1.0;
        ServerConfig.HolidayOverrides o = ServerConfig.INSTANCE.holidays.get(id.substring(EverFest.MOD_ID.length() + 1));
        return o == null ? 1.0 : o.intensity.get();
    }
}
