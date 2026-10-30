package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.calendar.SubSeason;

/**
 * A point inside a sub-season's month: fraction 0 = its first day, 1 = its last day. Useful for season-anchored
 * holidays such as "Easter in mid spring" in Minecraft-time worlds.
 */
public record SeasonPointRule(SubSeason subSeason, double fraction) implements DateRule {
    public SeasonPointRule {
        if (!(fraction >= 0 && fraction <= 1)) throw new IllegalArgumentException("fraction must be within 0..1");
    }

    @Override
    public CalendarDate resolve(int year, CalendarSystem system) {
        int month = subSeason.month();
        int len = system.monthLength(year, month);
        int day = 1 + (int) Math.floor(fraction * (len - 1) + 1e-9);
        return system.clamp(year, month, day);
    }

    @Override
    public String describe() {
        return "season:" + subSeason.id() + ":" + fraction;
    }
}
