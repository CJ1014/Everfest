package com.holidayseasons.holiday.rule;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;

import java.time.LocalDate;

/** Western (Gregorian) Easter Sunday plus an offset in days, mapped onto the native calendar. */
public record EasterRule(int offsetDays) implements DateRule {
    public EasterRule {
        if (Math.abs(offsetDays) > 60) throw new IllegalArgumentException("offset must be within 60 days");
    }

    /** Anonymous Gregorian computus (Meeus/Jones/Butcher). */
    public static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    @Override
    public CalendarDate resolve(int year, CalendarSystem system) {
        LocalDate g = easterSunday(system.referenceGregorianYear(year)).plusDays(offsetDays);
        if (system.isGregorian()) return new CalendarDate(g.getYear(), g.getMonthValue(), g.getDayOfMonth());
        return system.mapGregorian(year, g.getMonthValue(), g.getDayOfMonth(), g.lengthOfMonth());
    }

    @Override
    public String describe() {
        return "easter:" + offsetDays;
    }
}
