package com.holidayseasons.calendar;

import com.holidayseasons.seasons.SeasonReading;
import org.junit.jupiter.api.Test;

import static com.holidayseasons.calendar.ClockMath.TICKS_PER_DAY;
import static org.junit.jupiter.api.Assertions.*;

class SeasonAlignmentTest {
    static final long SUB = 8 * TICKS_PER_DAY;

    static SeasonReading reading(long cycle) {
        return new SeasonReading(cycle, SUB, TICKS_PER_DAY, SeasonCycleMapper.subSeasonAt(cycle, SUB), true);
    }

    static int dayOfYear(long cycle, long worldDayTime) {
        return SeasonCycleMapper.dayOfYear(CalendarService.alignedCycle(reading(cycle), worldDayTime), SUB, TICKS_PER_DAY);
    }

    @Test
    void alignedClocksGiveSeasonDay() {
        // SS and world in step: every tick of SS day N is EverFest day N.
        for (long t = 0; t < TICKS_PER_DAY; t += 1000) {
            long cycle = 5 * TICKS_PER_DAY + t;
            assertEquals(SeasonCycleMapper.dayOfYear(cycle, SUB, TICKS_PER_DAY), dayOfYear(cycle, 7 * TICKS_PER_DAY + t));
        }
    }

    @Test
    void misalignedClocksRollAtWorldDawn() {
        // After '/season set' at world time 9000 the SS cycle sits at a day start while the world is mid-day.
        long cycleAtSet = 10 * TICKS_PER_DAY;
        int dayDuringFirstWorldDay = dayOfYear(cycleAtSet, 9000);
        // Until the next world dawn the date must not change...
        assertEquals(dayDuringFirstWorldDay, dayOfYear(cycleAtSet + (TICKS_PER_DAY - 9000) - 1, TICKS_PER_DAY - 1));
        // ...and it changes exactly at world dawn.
        assertEquals(dayDuringFirstWorldDay + 1, dayOfYear(cycleAtSet + (TICKS_PER_DAY - 9000), 0));
    }

    @Test
    void setDateCompensationLandsOnTarget() {
        long subStart = 7 * SUB; // e.g. MID_AUTUMN start
        int firstDayOfSub = SeasonCycleMapper.dayOfYear(subStart, SUB, TICKS_PER_DAY);
        for (long tod = 0; tod < TICKS_PER_DAY; tod += 500) {
            for (int targetDay = 1; targetDay <= 8; targetDay++) {
                long addDays = (targetDay - 1L) + (tod > TICKS_PER_DAY / 2 ? 1 : 0);
                long cycle = subStart + addDays * TICKS_PER_DAY;
                long worldTime = tod + addDays * TICKS_PER_DAY;
                assertEquals(firstDayOfSub + targetDay - 1, dayOfYear(cycle, worldTime), "tod " + tod + " target day " + targetDay);
            }
        }
    }
}
