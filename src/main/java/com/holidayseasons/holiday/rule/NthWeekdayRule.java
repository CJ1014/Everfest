package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * The n-th given weekday of a Gregorian month (n = -1 for the last), e.g. the 4th Thursday of November.
 * Resolved in the reference Gregorian year, then mapped onto the native calendar.
 */
public record NthWeekdayRule(int month, DayOfWeek weekday, int n) implements DateRule {
    public NthWeekdayRule {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month out of range: " + month);
        if (n == 0 || n < -1 || n > 5) throw new IllegalArgumentException("n must be 1..5 or -1: " + n);
    }

    public LocalDate gregorian(int gregorianYear) {
        LocalDate first = LocalDate.of(gregorianYear, month, 1);
        if (n == -1) return first.with(TemporalAdjusters.lastInMonth(weekday));
        LocalDate d = first.with(TemporalAdjusters.dayOfWeekInMonth(n, weekday));
        // A 5th weekday may not exist; fall back to the last one in the month.
        return d.getMonthValue() == month ? d : first.with(TemporalAdjusters.lastInMonth(weekday));
    }

    @Override
    public CalendarDate resolve(int year, CalendarSystem system) {
        LocalDate g = gregorian(system.referenceGregorianYear(year));
        return system.mapGregorian(year, g.getMonthValue(), g.getDayOfMonth(), g.lengthOfMonth());
    }

    @Override
    public String describe() {
        return "nth_weekday:" + month + ":" + weekday.name().toLowerCase() + ":" + n;
    }
}
