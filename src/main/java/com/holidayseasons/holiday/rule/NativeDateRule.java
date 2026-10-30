package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;

/** An exact native month/day with no scaling; clamped to the month's length. */
public record NativeDateRule(int month, int day) implements DateRule {
    public NativeDateRule {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month out of range: " + month);
        if (day < 1) throw new IllegalArgumentException("day must be >= 1");
    }

    @Override
    public CalendarDate resolve(int year, CalendarSystem system) {
        return system.clamp(year, month, day);
    }

    @Override
    public String describe() {
        return "native:" + month + "-" + day;
    }
}
