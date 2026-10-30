package com.holidayseasons.calendar;

import java.util.Locale;

/**
 * An immutable calendar date. Months and days are 1-based. The meaning of a date depends on the
 * {@link CalendarSystem} that produced it, so dates from different systems must not be compared.
 */
public record CalendarDate(int year, int month, int day) implements Comparable<CalendarDate> {
    public static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    public CalendarDate {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month out of range: " + month);
        if (day < 1) throw new IllegalArgumentException("day out of range: " + day);
    }

    @Override
    public int compareTo(CalendarDate o) {
        if (year != o.year) return Integer.compare(year, o.year);
        if (month != o.month) return Integer.compare(month, o.month);
        return Integer.compare(day, o.day);
    }

    public String monthName() {
        return MONTH_NAMES[month - 1];
    }

    /** ISO-like stable form, e.g. {@code 0003-10-31}. Used in commands, logs and persistence keys. */
    public String toIsoString() {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day);
    }

    /** Human readable English form, e.g. {@code October 31, Year 3}. Clients use translations instead. */
    public String toDisplayString(boolean realWorld) {
        return realWorld
                ? monthName() + " " + day + ", " + year
                : monthName() + " " + day + ", Year " + year;
    }

    public static CalendarDate parseIso(String s) {
        String[] parts = s.trim().split("-");
        if (parts.length != 3) throw new IllegalArgumentException("Expected YYYY-MM-DD: " + s);
        return new CalendarDate(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public String toString() {
        return toIsoString();
    }
}
