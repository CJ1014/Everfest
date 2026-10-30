package com.holidayseasons.holiday;

/**
 * The epoch-day boundaries of one occurrence: [prepStart, start) preparing, [start, end) active,
 * [end, cleanupEnd) cleanup, then completed.
 */
public record HolidayWindow(long prepStart, long start, long end, long cleanupEnd) {
    public HolidayWindow {
        if (!(prepStart <= start && start < end && end <= cleanupEnd)) {
            throw new IllegalArgumentException("Invalid window " + prepStart + "/" + start + "/" + end + "/" + cleanupEnd);
        }
    }

    public HolidayPhase phaseAt(long epochDay) {
        if (epochDay < prepStart) return HolidayPhase.INACTIVE;
        if (epochDay < start) return HolidayPhase.PREPARING;
        if (epochDay < end) return HolidayPhase.ACTIVE;
        if (epochDay < cleanupEnd) return HolidayPhase.CLEANUP;
        return HolidayPhase.COMPLETED;
    }

    public boolean isLiveOn(long epochDay) {
        return epochDay >= prepStart && epochDay < cleanupEnd;
    }
}
