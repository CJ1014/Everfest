package com.holidayseasons.seasons;

import com.holidayseasons.EverFest;
import net.neoforged.fml.ModList;

/** Chooses the season provider at startup. Serene Seasons classes are only touched when the mod is loaded. */
public final class SeasonAdapters {
    public static final String SERENE_SEASONS_ID = "sereneseasons";
    private static SeasonAdapter active = SeasonAdapter.NONE;

    private SeasonAdapters() {
    }

    public static void init() {
        if (ModList.get().isLoaded(SERENE_SEASONS_ID)) {
            try {
                active = com.holidayseasons.compat.sereneseasons.SereneSeasonsAdapter.create();
                EverFest.LOGGER.info("EverFest: Serene Seasons integration enabled");
            } catch (LinkageError | RuntimeException e) {
                active = SeasonAdapter.NONE;
                EverFest.LOGGER.error("EverFest: Serene Seasons is installed but its API is incompatible; seasons will follow the calendar", e);
            }
        } else {
            EverFest.LOGGER.info("EverFest: Serene Seasons not installed; seasons will follow the calendar");
        }
    }

    public static SeasonAdapter get() {
        return active;
    }
}
