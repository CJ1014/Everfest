package com.holidayseasons.calendar;

/** Pure tick arithmetic for the Minecraft-time calendar clock. */
public final class ClockMath {
    public static final long TICKS_PER_DAY = 24000L;

    private ClockMath() {
    }

    /**
     * Returns the new calendar tick counter after the overworld day time changed from {@code oldDayTime} to
     * {@code newDayTime}. Forward motion (normal ticking, sleeping, {@code /time add}, forward {@code /time set})
     * always advances by the exact delta. Backward motion is resolved by {@code policy}.
     */
    public static long applyDayTimeChange(long calendarTicks, long oldDayTime, long newDayTime, TimeJumpPolicy policy) {
        long delta = newDayTime - oldDayTime;
        if (delta >= 0) return calendarTicks + delta;
        return switch (policy) {
            case FORWARD_TO_NEXT_TIME_OF_DAY -> calendarTicks + Math.floorMod(delta, TICKS_PER_DAY);
            case KEEP_DATE -> dayStart(calendarTicks) + Math.floorMod(newDayTime, TICKS_PER_DAY);
            case REWIND -> Math.max(Math.floorMod(newDayTime, TICKS_PER_DAY), calendarTicks + delta);
        };
    }

    public static long dayStart(long ticks) {
        return Math.floorDiv(ticks, TICKS_PER_DAY) * TICKS_PER_DAY;
    }

    public static long dayIndex(long ticks) {
        return Math.floorDiv(ticks, TICKS_PER_DAY);
    }

    public static int timeOfDay(long ticks) {
        return (int) Math.floorMod(ticks, TICKS_PER_DAY);
    }

    /** Minecraft time-of-day (0 = 06:00) to a 24h clock hour. */
    public static int hourOf(int timeOfDay) {
        return (int) (Math.floorMod(timeOfDay + 6000L, TICKS_PER_DAY) / 1000L);
    }

    public static int minuteOf(int timeOfDay) {
        return (int) ((Math.floorMod(timeOfDay + 6000L, TICKS_PER_DAY) % 1000L) * 60L / 1000L);
    }

    /** Real-world wall clock to Minecraft time-of-day ticks (midnight = 18000). */
    public static int timeOfDayFromWallClock(int hour, int minute, int second) {
        long ticks = hour * 1000L + (minute * 60L + second) * 1000L / 3600L - 6000L;
        return (int) Math.floorMod(ticks, TICKS_PER_DAY);
    }

    /**
     * Tracks a position on a cyclic year (Serene Seasons cycle) and returns the updated year counter.
     * Movements shorter than half a cycle are treated as forward; longer ones as backward.
     */
    public static int trackCycleYear(int year, long oldPos, long newPos, long cycleLength) {
        if (cycleLength <= 0 || oldPos == newPos) return year;
        long forward = Math.floorMod(newPos - oldPos, cycleLength);
        if (forward <= cycleLength / 2) {
            return newPos < oldPos ? year + 1 : year;
        }
        return newPos > oldPos ? Math.max(1, year - 1) : year;
    }
}
