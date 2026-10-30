package com.holidayseasons.calendar;

/**
 * Maps a Serene Seasons cycle position onto EverFest's 12-month year. Serene Seasons' cycle starts at
 * EARLY_SPRING, which EverFest treats as March 1, so January 1 sits two sub-seasons before the cycle start.
 * Pure arithmetic so it can be unit tested without Serene Seasons on the classpath.
 */
public final class SeasonCycleMapper {
    private SeasonCycleMapper() {
    }

    /** Ticks since January 1 of the current EverFest year. */
    public static long yearTicks(long cycleTicks, long subSeasonTicks) {
        long cycle = subSeasonTicks * 12L;
        return Math.floorMod(cycleTicks + (SubSeason.CYCLE_START_MONTH - 1L) * subSeasonTicks, cycle);
    }

    /** Day index (0-based) since January 1 for a given cycle position. */
    public static int dayOfYear(long cycleTicks, long subSeasonTicks, long dayDuration) {
        return (int) (yearTicks(cycleTicks, subSeasonTicks) / Math.max(1L, dayDuration));
    }

    public static CalendarDate dateFor(int year, long cycleTicks, long subSeasonTicks, long dayDuration, FixedMonthCalendarSystem system) {
        long firstDay = system.toEpochDay(new CalendarDate(year, 1, 1));
        return system.fromEpochDay(firstDay + dayOfYear(cycleTicks, subSeasonTicks, dayDuration));
    }

    /** Sub-season index (0 = EARLY_SPRING) at a cycle position, identical to Serene Seasons' own formula. */
    public static SubSeason subSeasonAt(long cycleTicks, long subSeasonTicks) {
        return SubSeason.byOrdinal((int) (Math.floorMod(cycleTicks, subSeasonTicks * 12L) / subSeasonTicks));
    }
}
