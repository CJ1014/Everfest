package com.holidayseasons.calendar;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * A deterministic Minecraft-time calendar with configurable month lengths and an optional leap rule.
 * Years start at 1; epoch day 0 is Year 1, January 1. Weekdays cycle every 7 days with epoch day 0 a Monday.
 *
 * <p>Used both for the Serene-Seasons-synchronised calendar (12 months, each one sub-season long) and
 * for fully custom long calendars.</p>
 */
public final class FixedMonthCalendarSystem implements CalendarSystem {
    private final int[] monthLengths;
    /** Every {@code leapInterval}-th year gains one day in {@link #leapMonth}. 0 disables leap years. */
    private final int leapInterval;
    private final int leapMonth;
    private final int referenceYearBase;
    private final int baseYearLength;
    private final long leapCycleDays;
    private final String signature;

    public FixedMonthCalendarSystem(int[] monthLengths, int leapInterval, int leapMonth, int referenceYearBase) {
        if (monthLengths.length != 12) throw new IllegalArgumentException("Exactly 12 month lengths required");
        for (int len : monthLengths) {
            if (len < 1 || len > 1000) throw new IllegalArgumentException("Month length out of range: " + len);
        }
        if (leapInterval < 0) throw new IllegalArgumentException("leapInterval must be >= 0");
        if (leapMonth < 1 || leapMonth > 12) throw new IllegalArgumentException("leapMonth out of range");
        this.monthLengths = monthLengths.clone();
        this.leapInterval = leapInterval;
        this.leapMonth = leapMonth;
        this.referenceYearBase = referenceYearBase;
        int total = 0;
        for (int len : monthLengths) total += len;
        this.baseYearLength = total;
        this.leapCycleDays = leapInterval > 0 ? (long) baseYearLength * leapInterval + 1 : baseYearLength;
        StringJoiner j = new StringJoiner(",");
        for (int len : monthLengths) j.add(Integer.toString(len));
        this.signature = "fixed[" + j + "]L" + leapInterval + "M" + leapMonth;
    }

    public static FixedMonthCalendarSystem uniform(int daysPerMonth, int referenceYearBase) {
        int[] lengths = new int[12];
        Arrays.fill(lengths, daysPerMonth);
        return new FixedMonthCalendarSystem(lengths, 0, 2, referenceYearBase);
    }

    public boolean isLeapYear(int year) {
        return leapInterval > 0 && year % leapInterval == 0;
    }

    @Override
    public String signature() {
        return signature;
    }

    @Override
    public int monthLength(int year, int month) {
        int len = monthLengths[month - 1];
        if (month == leapMonth && isLeapYear(year)) len++;
        return len;
    }

    @Override
    public int yearLength(int year) {
        return baseYearLength + (isLeapYear(year) ? 1 : 0);
    }

    /** Days before the start of {@code year} (year >= 1). */
    private long daysBeforeYear(int year) {
        long y = year - 1L;
        if (leapInterval <= 0) return y * baseYearLength;
        return y * baseYearLength + Math.floorDiv(y, leapInterval);
    }

    @Override
    public long toEpochDay(CalendarDate date) {
        long days = daysBeforeYear(date.year());
        for (int m = 1; m < date.month(); m++) days += monthLength(date.year(), m);
        return days + date.day() - 1;
    }

    @Override
    public CalendarDate fromEpochDay(long epochDay) {
        int year;
        long remaining;
        if (leapInterval > 0) {
            long cycles = Math.floorDiv(epochDay, leapCycleDays);
            remaining = Math.floorMod(epochDay, leapCycleDays);
            year = (int) (cycles * leapInterval) + 1;
        } else {
            year = (int) Math.floorDiv(epochDay, baseYearLength) + 1;
            remaining = Math.floorMod(epochDay, baseYearLength);
        }
        while (remaining >= yearLength(year)) {
            remaining -= yearLength(year);
            year++;
        }
        int month = 1;
        while (remaining >= monthLength(year, month)) {
            remaining -= monthLength(year, month);
            month++;
        }
        return new CalendarDate(year, month, (int) remaining + 1);
    }

    @Override
    public int dayOfWeek(long epochDay) {
        return (int) Math.floorMod(epochDay, 7L) + 1;
    }

    @Override
    public boolean isGregorian() {
        return false;
    }

    @Override
    public int referenceGregorianYear(int year) {
        return referenceYearBase + year;
    }

    public int[] monthLengths() {
        return monthLengths.clone();
    }
}
