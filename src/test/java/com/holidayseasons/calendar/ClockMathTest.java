package com.holidayseasons.calendar;

import org.junit.jupiter.api.Test;

import static com.holidayseasons.calendar.ClockMath.TICKS_PER_DAY;
import static org.junit.jupiter.api.Assertions.*;

class ClockMathTest {

    @Test
    void normalTickingAdvancesByDelta() {
        assertEquals(101, ClockMath.applyDayTimeChange(100, 5000, 5001, TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY));
    }

    @Test
    void sleepingSkipsToNextMorning() {
        // Vanilla sleep: dayTime 13500 -> 24000 (next morning)
        long cal = 5 * TICKS_PER_DAY + 13500;
        long after = ClockMath.applyDayTimeChange(cal, 13500, 24000, TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY);
        assertEquals(6, ClockMath.dayIndex(after));
        assertEquals(0, ClockMath.timeOfDay(after));
    }

    @Test
    void timeAddAdvancesDays() {
        long cal = 2 * TICKS_PER_DAY + 1000;
        long after = ClockMath.applyDayTimeChange(cal, 49000, 49000 + 3 * TICKS_PER_DAY, TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY);
        assertEquals(5, ClockMath.dayIndex(after));
        assertEquals(1000, ClockMath.timeOfDay(after));
    }

    @Test
    void timeSetDayAfterManyDaysDoesNotRewindByDefault() {
        // World at absolute day 40 noon; admin runs /time set day (absolute 1000).
        long cal = 40 * TICKS_PER_DAY + 6000;
        long oldDayTime = 40 * TICKS_PER_DAY + 6000;
        long after = ClockMath.applyDayTimeChange(cal, oldDayTime, 1000, TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY);
        assertEquals(41, ClockMath.dayIndex(after), "moves forward to next morning");
        assertEquals(1000, ClockMath.timeOfDay(after));
        assertTrue(after > cal);
    }

    @Test
    void keepDatePolicyOnlyChangesTimeOfDay() {
        long cal = 40 * TICKS_PER_DAY + 18000;
        long after = ClockMath.applyDayTimeChange(cal, 40 * TICKS_PER_DAY + 18000, 1000, TimeJumpPolicy.KEEP_DATE);
        assertEquals(40, ClockMath.dayIndex(after));
        assertEquals(1000, ClockMath.timeOfDay(after));
    }

    @Test
    void rewindPolicyRewindsButNeverBelowZero() {
        long cal = 3 * TICKS_PER_DAY + 500;
        long after = ClockMath.applyDayTimeChange(cal, 3 * TICKS_PER_DAY + 500, TICKS_PER_DAY + 500, TimeJumpPolicy.REWIND);
        assertEquals(TICKS_PER_DAY + 500, after);
        long clamped = ClockMath.applyDayTimeChange(500, 100 * TICKS_PER_DAY, 1000, TimeJumpPolicy.REWIND);
        assertTrue(clamped >= 0);
    }

    @Test
    void wallClockConversion() {
        assertEquals(18000, ClockMath.timeOfDayFromWallClock(0, 0, 0), "midnight");
        assertEquals(0, ClockMath.timeOfDayFromWallClock(6, 0, 0), "06:00");
        assertEquals(6000, ClockMath.timeOfDayFromWallClock(12, 0, 0), "noon");
        assertEquals(0, ClockMath.hourOf(18000));
        assertEquals(6, ClockMath.hourOf(0));
        assertEquals(30, ClockMath.minuteOf(500));
    }

    @Test
    void cycleYearTracking() {
        long cycle = 96 * TICKS_PER_DAY;
        assertEquals(1, ClockMath.trackCycleYear(1, 100, 101, cycle));
        assertEquals(2, ClockMath.trackCycleYear(1, cycle - 1, 0, cycle), "wrap forward increments year");
        assertEquals(1, ClockMath.trackCycleYear(2, 0, cycle - 1, cycle), "small backward wrap decrements year");
        assertEquals(1, ClockMath.trackCycleYear(1, 0, 0, cycle));
        assertEquals(1, ClockMath.trackCycleYear(1, 0, cycle - 10, cycle), "never below year 1");
        // Jumping 3/4 of the cycle forward without wrapping is interpreted as moving backward without wrap
        assertEquals(1, ClockMath.trackCycleYear(1, 10, 10 + cycle * 3 / 4, cycle));
    }

    @Test
    void seasonCycleMapping() {
        long sub = 8 * TICKS_PER_DAY;
        FixedMonthCalendarSystem s = FixedMonthCalendarSystem.uniform(8, 2000);
        // SS cycle tick 0 = EARLY_SPRING start = March 1
        assertEquals(new CalendarDate(1, 3, 1), SeasonCycleMapper.dateFor(1, 0, sub, TICKS_PER_DAY, s));
        assertEquals(SubSeason.EARLY_SPRING, SeasonCycleMapper.subSeasonAt(0, sub));
        // Mid winter start (sub-season 10) = January 1
        assertEquals(new CalendarDate(1, 1, 1), SeasonCycleMapper.dateFor(1, 10 * sub, sub, TICKS_PER_DAY, s));
        assertEquals(SubSeason.MID_WINTER, SeasonCycleMapper.subSeasonAt(10 * sub, sub));
        // Last day of mid autumn = October 8 (Halloween in the default 96-day year)
        long lastDayMidAutumn = 8 * sub - TICKS_PER_DAY;
        assertEquals(new CalendarDate(1, 10, 8), SeasonCycleMapper.dateFor(1, lastDayMidAutumn, sub, TICKS_PER_DAY, s));
        assertEquals(SubSeason.MID_AUTUMN, SeasonCycleMapper.subSeasonAt(lastDayMidAutumn, sub));
        for (int m = 1; m <= 12; m++) {
            assertEquals(m, SubSeason.forMonth(m).month());
        }
        assertEquals(SubSeason.MID_AUTUMN, SubSeason.forMonth(10));
        assertEquals(SubSeason.EARLY_WINTER, SubSeason.forMonth(12));
    }
}
