package com.holidayseasons.calendar;

import java.util.Locale;

/**
 * Twelve sub-seasons in the same order as Serene Seasons (which starts its cycle at early spring).
 * In the Northern-hemisphere convention used by EverFest, March is early spring and January is mid winter.
 */
public enum SubSeason {
    EARLY_SPRING(Season.SPRING), MID_SPRING(Season.SPRING), LATE_SPRING(Season.SPRING),
    EARLY_SUMMER(Season.SUMMER), MID_SUMMER(Season.SUMMER), LATE_SUMMER(Season.SUMMER),
    EARLY_AUTUMN(Season.AUTUMN), MID_AUTUMN(Season.AUTUMN), LATE_AUTUMN(Season.AUTUMN),
    EARLY_WINTER(Season.WINTER), MID_WINTER(Season.WINTER), LATE_WINTER(Season.WINTER);

    /** Calendar month (1-12) at which Serene Seasons' cycle (EARLY_SPRING) begins. */
    public static final int CYCLE_START_MONTH = 3;

    private final Season season;

    SubSeason(Season season) {
        this.season = season;
    }

    public Season season() {
        return season;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The calendar month that corresponds to this sub-season (EARLY_SPRING -> March). */
    public int month() {
        return Math.floorMod(ordinal() + CYCLE_START_MONTH - 1, 12) + 1;
    }

    public static SubSeason forMonth(int month) {
        return values()[Math.floorMod(month - CYCLE_START_MONTH, 12)];
    }

    public static SubSeason byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, 12)];
    }
}
