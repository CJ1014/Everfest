package com.holidayseasons.holiday;

import com.holidayseasons.calendar.CalendarDate;

/**
 * Client-facing summary of one holiday occurrence.
 *
 * @param id             holiday id, e.g. {@code holidayseasons:halloween}
 * @param color          ARGB accent color for UI
 * @param prepStart      first preparation day
 * @param start          first celebration day
 * @param end            last celebration day (inclusive)
 * @param phase          current lifecycle phase
 * @param daysUntilStart days from today until {@code start} (negative once started)
 * @param skippedReason  non-empty if this year's occurrence was skipped (e.g. season mismatch)
 */
public record HolidayStatus(String id, int color, CalendarDate prepStart, CalendarDate start, CalendarDate end,
                            HolidayPhase phase, long daysUntilStart, String skippedReason) {
    public String translationKey() {
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        return "holiday." + ns + "." + path;
    }

    public boolean covers(CalendarDate date) {
        return date.compareTo(start) >= 0 && date.compareTo(end) <= 0;
    }

    public boolean preparingOn(CalendarDate date) {
        return date.compareTo(prepStart) >= 0 && date.compareTo(start) < 0;
    }
}
