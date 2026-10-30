package com.holidayseasons.holiday;

/**
 * One lifecycle step emitted by the engine.
 *
 * @param kind        ENTER: normal forward step; ABORT: the occurrence ended early (date moved back before it,
 *                    holiday disabled, calendar changed) and its world changes must be cleaned up;
 *                    SKIP: the occurrence will not be celebrated this year (season mismatch)
 * @param transientStep true if this phase is passed through in the same reconcile (the date jumped over it);
 *                    listeners should skip expensive world work for transient steps
 */
public record HolidayTransition(String holidayId, String occurrenceKey, int year, HolidayPhase from, HolidayPhase to,
                                Kind kind, boolean transientStep, String reason) {
    public enum Kind { ENTER, ABORT, SKIP }
}
