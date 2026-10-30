package com.holidayseasons.calendar;

/**
 * A deterministic mapping between epoch days and {@link CalendarDate}s.
 *
 * <p>Holiday date rules are written in Gregorian terms (e.g. October 31, 4th Thursday of November).
 * Non-Gregorian systems resolve rules against a reference Gregorian year and then map the
 * resulting Gregorian month/day proportionally onto their own month lengths with
 * {@link #mapGregorian(int, int, int, int)}. This keeps Halloween at the end of October even when
 * October only has 8 days.</p>
 */
public interface CalendarSystem {
    /** Stable identifier used in persistence keys so occurrences from different systems never collide. */
    String signature();

    int monthLength(int year, int month);

    default int yearLength(int year) {
        int total = 0;
        for (int m = 1; m <= 12; m++) total += monthLength(year, m);
        return total;
    }

    long toEpochDay(CalendarDate date);

    CalendarDate fromEpochDay(long epochDay);

    /** ISO day of week, 1 = Monday ... 7 = Sunday. */
    int dayOfWeek(long epochDay);

    boolean isGregorian();

    /** Gregorian year used to evaluate rule-based holiday dates (Easter, Nth weekday) for a native year. */
    int referenceGregorianYear(int year);

    default CalendarDate plusDays(CalendarDate date, long days) {
        return fromEpochDay(toEpochDay(date) + days);
    }

    default CalendarDate clamp(int year, int month, int day) {
        int len = monthLength(year, month);
        return new CalendarDate(year, month, Math.max(1, Math.min(len, day)));
    }

    /**
     * Maps a Gregorian month/day onto this system's native year. Identity for Gregorian systems and for
     * native months of the same length; otherwise proportional: {@code floor((gDay-1) * nativeLen / gLen) + 1}.
     */
    default CalendarDate mapGregorian(int year, int gMonth, int gDay, int gregorianMonthLength) {
        int nativeLen = monthLength(year, gMonth);
        if (isGregorian() || nativeLen == gregorianMonthLength) {
            return clamp(year, gMonth, gDay);
        }
        int scaled = (int) (((long) (gDay - 1) * nativeLen) / gregorianMonthLength) + 1;
        return clamp(year, gMonth, scaled);
    }

    default boolean isValid(CalendarDate date) {
        return date.day() <= monthLength(date.year(), date.month());
    }
}
