package com.holidayseasons.holiday;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.FixedMonthCalendarSystem;
import com.holidayseasons.calendar.GregorianCalendarSystem;
import com.holidayseasons.calendar.SubSeason;
import com.holidayseasons.holiday.rule.DateRule;
import com.holidayseasons.holiday.rule.DateRules;
import com.holidayseasons.holiday.rule.EasterRule;
import com.holidayseasons.holiday.rule.FixedDateRule;
import com.holidayseasons.holiday.rule.NativeDateRule;
import com.holidayseasons.holiday.rule.NthWeekdayRule;
import com.holidayseasons.holiday.rule.SeasonPointRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateRuleTest {
    private static final GregorianCalendarSystem G = GregorianCalendarSystem.INSTANCE;
    private static final FixedMonthCalendarSystem SS = FixedMonthCalendarSystem.uniform(8, 2000);

    @Test
    void easterComputusKnownDates() {
        assertEquals(LocalDate.of(2024, 3, 31), EasterRule.easterSunday(2024));
        assertEquals(LocalDate.of(2025, 4, 20), EasterRule.easterSunday(2025));
        assertEquals(LocalDate.of(2026, 4, 5), EasterRule.easterSunday(2026));
        assertEquals(LocalDate.of(2027, 3, 28), EasterRule.easterSunday(2027));
        assertEquals(LocalDate.of(2038, 4, 25), EasterRule.easterSunday(2038));
        assertEquals(LocalDate.of(2285, 3, 22), EasterRule.easterSunday(2285));
        for (int y = 1900; y < 2400; y++) {
            LocalDate e = EasterRule.easterSunday(y);
            assertEquals(DayOfWeek.SUNDAY, e.getDayOfWeek(), "Easter " + y + " is a Sunday");
            assertTrue(!e.isBefore(LocalDate.of(y, 3, 22)) && !e.isAfter(LocalDate.of(y, 4, 25)));
        }
    }

    @Test
    void thanksgivingIsFourthThursdayOfNovember() {
        NthWeekdayRule r = new NthWeekdayRule(11, DayOfWeek.THURSDAY, 4);
        assertEquals(new CalendarDate(2026, 11, 26), r.resolve(2026, G));
        assertEquals(new CalendarDate(2025, 11, 27), r.resolve(2025, G));
        assertEquals(new CalendarDate(2024, 11, 28), r.resolve(2024, G));
    }

    @Test
    void lastAndFifthWeekday() {
        assertEquals(new CalendarDate(2026, 5, 25), new NthWeekdayRule(5, DayOfWeek.MONDAY, -1).resolve(2026, G));
        // February 2026 has no 5th Monday -> last Monday
        assertEquals(new CalendarDate(2026, 2, 23), new NthWeekdayRule(2, DayOfWeek.MONDAY, 5).resolve(2026, G));
    }

    @Test
    void fixedRulesInGregorianAndShortMonths() {
        assertEquals(new CalendarDate(2026, 10, 31), new FixedDateRule(10, 31).resolve(2026, G));
        assertEquals(new CalendarDate(3, 10, 8), new FixedDateRule(10, 31).resolve(3, SS));
        assertEquals(new CalendarDate(3, 12, 7), new FixedDateRule(12, 25).resolve(3, SS));
        assertEquals(new CalendarDate(2027, 2, 28), new FixedDateRule(2, 29).resolve(2027, G), "Feb 29 clamps in common years");
        assertEquals(new CalendarDate(2028, 2, 29), new FixedDateRule(2, 29).resolve(2028, G));
    }

    @Test
    void ruleBasedDatesMapIntoShortMonths() {
        // Easter 2003 (reference year for game year 3) = April 20 -> scaled into an 8-day April
        CalendarDate easter = new EasterRule(0).resolve(3, SS);
        assertEquals(4, easter.month());
        assertEquals(1 + (19 * 8) / 30, easter.day());
        CalendarDate tg = new NthWeekdayRule(11, DayOfWeek.THURSDAY, 4).resolve(3, SS);
        assertEquals(11, tg.month());
        assertTrue(tg.day() >= 6 && tg.day() <= 8);
    }

    @Test
    void nativeAndSeasonPointRules() {
        assertEquals(new CalendarDate(1, 10, 8), new NativeDateRule(10, 31).resolve(1, SS), "clamped");
        assertEquals(new CalendarDate(1, 4, 1), new SeasonPointRule(SubSeason.MID_SPRING, 0).resolve(1, SS));
        assertEquals(new CalendarDate(1, 4, 8), new SeasonPointRule(SubSeason.MID_SPRING, 1).resolve(1, SS));
        assertEquals(new CalendarDate(1, 4, 4), new SeasonPointRule(SubSeason.MID_SPRING, 0.5).resolve(1, SS));
    }

    @Test
    void parseAndDescribeRoundTrip() {
        String[] rules = {"fixed:10-31", "native:4-3", "nth_weekday:11:thursday:4", "nth_weekday:5:monday:-1", "easter:0", "easter:-2", "season:mid_spring:0.5"};
        for (String s : rules) {
            DateRule r = DateRules.parse(s);
            assertEquals(r, DateRules.parse(r.describe()), s);
        }
        assertTrue(DateRules.tryParse("fixed:13-01").error().isPresent());
        assertTrue(DateRules.tryParse("fixed:2-30").error().isPresent());
        assertTrue(DateRules.tryParse("bogus:1").error().isPresent());
        assertTrue(DateRules.tryParse("nth_weekday:11:funday:4").error().isPresent());
        assertTrue(DateRules.tryParse("season:mid_spring:2").error().isPresent());
    }
}
