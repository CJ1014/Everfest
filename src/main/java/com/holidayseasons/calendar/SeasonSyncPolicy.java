package com.holidayseasons.calendar;

/**
 * How holidays are scheduled when the calendar date and the in-game season disagree (for example in
 * REAL_WORLD mode, or a CUSTOM calendar whose year length differs from Serene Seasons' cycle).
 */
public enum SeasonSyncPolicy {
    /** Holidays follow their calendar dates. Seasons are cosmetic. */
    FOLLOW_DATE,
    /**
     * Holidays follow seasonal alignment: they are scheduled on the Serene Seasons cycle (Halloween at the end of
     * mid autumn, Christmas in early winter) regardless of the displayed calendar date. Requires Serene Seasons;
     * falls back to FOLLOW_DATE without it.
     */
    FOLLOW_SEASON,
    /** Holidays follow their dates but are skipped for the year if the current season is not one they prefer. */
    REQUIRE_MATCHING_SEASON
}
