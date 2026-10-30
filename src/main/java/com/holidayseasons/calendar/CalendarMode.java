package com.holidayseasons.calendar;

/** How EverFest determines the current date. Chosen in the server config. */
public enum CalendarMode {
    /** Minecraft-time: the Serene Seasons year is divided into 12 months, one per sub-season. */
    SEASON_SYNCED,
    /** Minecraft-time: EverFest's own year with configurable month lengths and leap rule. */
    CUSTOM,
    /** The server's real-world date in its configured timezone, with optional admin overrides. */
    REAL_WORLD;

    public boolean isMinecraftTime() {
        return this != REAL_WORLD;
    }
}
