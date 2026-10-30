package com.holidayseasons.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CalendarSystemTest {

    @Test
    void gregorianMatchesJavaTime() {
        GregorianCalendarSystem g = GregorianCalendarSystem.INSTANCE;
        for (long day = -800; day < 30000; day += 37) {
            LocalDate ld = LocalDate.ofEpochDay(day);
            CalendarDate cd = g.fromEpochDay(day);
            assertEquals(ld.getYear(), cd.year());
            assertEquals(ld.getMonthValue(), cd.month());
            assertEquals(ld.getDayOfMonth(), cd.day());
            assertEquals(day, g.toEpochDay(cd));
            assertEquals(ld.getDayOfWeek().getValue(), g.dayOfWeek(day));
        }
    }

    @Test
    void gregorianLeapYears() {
        GregorianCalendarSystem g = GregorianCalendarSystem.INSTANCE;
        assertEquals(29, g.monthLength(2024, 2));
        assertEquals(28, g.monthLength(2026, 2));
        assertEquals(28, g.monthLength(1900, 2));
        assertEquals(29, g.monthLength(2000, 2));
        assertEquals(366, g.yearLength(2028));
        assertEquals(new CalendarDate(2028, 3, 1), g.plusDays(new CalendarDate(2028, 2, 28), 2));
        assertEquals(new CalendarDate(2027, 3, 1), g.plusDays(new CalendarDate(2027, 2, 28), 1));
    }

    @Test
    void uniformSystemRoundTripsAndMonthTransitions() {
        FixedMonthCalendarSystem s = FixedMonthCalendarSystem.uniform(8, 2000);
        assertEquals(96, s.yearLength(1));
        assertEquals(new CalendarDate(1, 1, 1), s.fromEpochDay(0));
        assertEquals(new CalendarDate(1, 1, 8), s.fromEpochDay(7));
        assertEquals(new CalendarDate(1, 2, 1), s.fromEpochDay(8));
        assertEquals(new CalendarDate(1, 12, 8), s.fromEpochDay(95));
        assertEquals(new CalendarDate(2, 1, 1), s.fromEpochDay(96));
        for (long d = 0; d < 5000; d++) {
            assertEquals(d, s.toEpochDay(s.fromEpochDay(d)), "round trip " + d);
        }
    }

    @Test
    void customSystemWithLeapRule() {
        int[] lengths = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        FixedMonthCalendarSystem s = new FixedMonthCalendarSystem(lengths, 4, 2, 2000);
        assertEquals(365, s.yearLength(1));
        assertEquals(366, s.yearLength(4));
        assertEquals(29, s.monthLength(4, 2));
        assertEquals(28, s.monthLength(5, 2));
        // Year 4 Feb 29 exists and is followed by March 1
        CalendarDate leapDay = new CalendarDate(4, 2, 29);
        assertTrue(s.isValid(leapDay));
        assertEquals(new CalendarDate(4, 3, 1), s.plusDays(leapDay, 1));
        assertFalse(s.isValid(new CalendarDate(3, 2, 29)));
        long prev = -1;
        for (long d = 0; d < 365 * 13; d++) {
            CalendarDate cd = s.fromEpochDay(d);
            assertEquals(d, s.toEpochDay(cd), "round trip " + d);
            assertTrue(s.isValid(cd));
            assertTrue(d > prev);
            prev = d;
        }
        // Year 5 starts after 4 * 365 + 1 days
        assertEquals(new CalendarDate(5, 1, 1), s.fromEpochDay(4 * 365 + 1));
    }

    @Test
    void consecutiveDatesAreStrictlyIncreasing() {
        FixedMonthCalendarSystem s = new FixedMonthCalendarSystem(new int[]{3, 5, 7, 2, 9, 1, 4, 6, 8, 10, 12, 11}, 3, 7, 2000);
        CalendarDate prev = s.fromEpochDay(0);
        for (long d = 1; d < 2000; d++) {
            CalendarDate cur = s.fromEpochDay(d);
            assertTrue(cur.compareTo(prev) > 0, cur + " after " + prev);
            prev = cur;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 8, 10, 30, 31, 40})
    void gregorianMappingIsMonotonicAndInRange(int nativeLen) {
        FixedMonthCalendarSystem s = FixedMonthCalendarSystem.uniform(nativeLen, 2000);
        int prev = 0;
        for (int gd = 1; gd <= 31; gd++) {
            CalendarDate mapped = s.mapGregorian(1, 10, gd, 31);
            assertEquals(10, mapped.month());
            assertTrue(mapped.day() >= 1 && mapped.day() <= nativeLen);
            assertTrue(mapped.day() >= prev);
            prev = mapped.day();
        }
        assertEquals(1, s.mapGregorian(1, 1, 1, 31).day(), "first day maps to first day");
    }

    @Test
    void holidayAnchorsInDefaultSeasonCalendar() {
        FixedMonthCalendarSystem s = FixedMonthCalendarSystem.uniform(8, 2000);
        assertEquals(new CalendarDate(1, 10, 8), s.mapGregorian(1, 10, 31, 31), "Halloween on last day of October");
        assertEquals(new CalendarDate(1, 12, 7), s.mapGregorian(1, 12, 25, 31), "Christmas late December");
        assertEquals(new CalendarDate(1, 1, 1), s.mapGregorian(1, 1, 1, 31), "New Year on January 1");
        assertEquals(new CalendarDate(1, 2, 4), s.mapGregorian(1, 2, 14, 28), "Valentine mid February");
    }

    @Test
    void gregorianMappingIsIdentity() {
        GregorianCalendarSystem g = GregorianCalendarSystem.INSTANCE;
        assertEquals(new CalendarDate(2026, 10, 31), g.mapGregorian(2026, 10, 31, 31));
        assertEquals(new CalendarDate(2026, 2, 28), g.mapGregorian(2026, 2, 29, 29), "clamped in non-leap year");
    }

    @Test
    void fixedWeekdaysCycle() {
        FixedMonthCalendarSystem s = FixedMonthCalendarSystem.uniform(8, 2000);
        assertEquals(1, s.dayOfWeek(0));
        assertEquals(7, s.dayOfWeek(6));
        assertEquals(1, s.dayOfWeek(7));
    }

    @Test
    void signaturesDifferBetweenSystems() {
        assertNotEquals(FixedMonthCalendarSystem.uniform(8, 2000).signature(), FixedMonthCalendarSystem.uniform(10, 2000).signature());
        assertNotEquals(GregorianCalendarSystem.INSTANCE.signature(), FixedMonthCalendarSystem.uniform(8, 2000).signature());
    }

    @Test
    void isoParseAndFormat() {
        CalendarDate d = CalendarDate.parseIso("2026-10-31");
        assertEquals(new CalendarDate(2026, 10, 31), d);
        assertEquals("2026-10-31", d.toIsoString());
        assertEquals("October 31, 2026", d.toDisplayString(true));
        assertEquals("October 31, Year 2026", d.toDisplayString(false));
        assertThrows(IllegalArgumentException.class, () -> CalendarDate.parseIso("2026-13-01"));
        assertThrows(IllegalArgumentException.class, () -> CalendarDate.parseIso("nope"));
    }
}
