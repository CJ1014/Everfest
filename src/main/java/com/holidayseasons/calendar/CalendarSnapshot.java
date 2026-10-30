package com.holidayseasons.calendar;

/**
 * Everything a client or gameplay system needs to know about "now". Produced by the server clock,
 * synchronised to clients, and consumed by the holiday engine.
 *
 * @param mode             active calendar mode
 * @param systemSignature  {@link CalendarSystem#signature()} of the system that produced {@code date}
 * @param date             current date
 * @param epochDay         epoch day of {@code date} in that system
 * @param timeOfDay        Minecraft time-of-day ticks (0 = 06:00, 18000 = midnight)
 * @param dayOfWeek        ISO day of week (1 = Monday)
 * @param season           current season
 * @param subSeason        current sub-season
 * @param seasonsFromSereneSeasons true if the season came from Serene Seasons rather than the calendar
 * @param monthLengths     month lengths of the current year (for the calendar GUI)
 */
public record CalendarSnapshot(
        CalendarMode mode,
        String systemSignature,
        CalendarDate date,
        long epochDay,
        int timeOfDay,
        int dayOfWeek,
        Season season,
        SubSeason subSeason,
        boolean seasonsFromSereneSeasons,
        int[] monthLengths
) {
    public int hour() {
        return ClockMath.hourOf(timeOfDay);
    }

    public int minute() {
        return ClockMath.minuteOf(timeOfDay);
    }

    public int monthLength() {
        return monthLengths[date.month() - 1];
    }

    public boolean isNight() {
        return timeOfDay >= 13000 && timeOfDay < 23000;
    }

    public static int[] monthLengthsOf(CalendarSystem system, int year) {
        int[] lengths = new int[12];
        for (int m = 1; m <= 12; m++) lengths[m - 1] = system.monthLength(year, m);
        return lengths;
    }
}
