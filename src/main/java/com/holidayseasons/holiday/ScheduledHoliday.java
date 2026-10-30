package com.holidayseasons.holiday;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.holiday.rule.DateRule;

import java.util.List;

/**
 * A holiday with config overrides applied, ready to schedule. Durations are clamped so one occurrence never spans
 * more than a year in tiny calendars.
 */
public record ScheduledHoliday(String id, DateRule rule, int preparationDays, int durationDays, int cleanupDays,
                               boolean enabled, List<Season> seasons, int color, HolidayDefinition definition) {

    public HolidayWindow window(int year, CalendarSystem system) {
        CalendarDate startDate = rule.resolve(year, system);
        long start = system.toEpochDay(startDate);
        int yearLen = Math.max(3, system.yearLength(year));
        int dur = Math.max(1, Math.min(durationDays, yearLen - 1));
        int prep = Math.max(0, Math.min(preparationDays, (yearLen - dur) / 2));
        int clean = Math.max(0, Math.min(cleanupDays, (yearLen - dur) / 2));
        return new HolidayWindow(start - prep, start, start + dur, start + dur + clean);
    }

    public boolean prefersSeason(Season season) {
        return seasons.isEmpty() || seasons.contains(season);
    }
}
