package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;

import java.time.YearMonth;

/** A Gregorian month/day (e.g. October 31), scaled onto shorter or longer native months. */
public record FixedDateRule(int month, int day) implements DateRule {
    public FixedDateRule {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month out of range: " + month);
        if (day < 1 || day > YearMonth.of(2000, month).lengthOfMonth()) throw new IllegalArgumentException("day out of range: " + day);
    }

    @Override
    public CalendarDate resolve(int year, CalendarSystem system) {
        int refYear = system.referenceGregorianYear(year);
        int gLen = YearMonth.of(refYear, month).lengthOfMonth();
        return system.mapGregorian(year, month, Math.min(day, gLen), gLen);
    }

    @Override
    public String describe() {
        return "fixed:" + month + "-" + day;
    }
}
