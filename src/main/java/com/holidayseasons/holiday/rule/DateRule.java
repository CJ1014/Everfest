package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;

/**
 * Determines a holiday's first celebration day for a given calendar year. Rules are deterministic: the same year and
 * calendar system always produce the same date.
 */
public sealed interface DateRule permits FixedDateRule, NthWeekdayRule, EasterRule, NativeDateRule, SeasonPointRule {
    CalendarDate resolve(int year, CalendarSystem system);

    /** Round-trippable text form understood by {@link DateRules#parse(String)}. */
    String describe();
}
