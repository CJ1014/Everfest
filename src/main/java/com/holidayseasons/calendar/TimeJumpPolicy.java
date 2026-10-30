package com.holidayseasons.calendar;

/** What the CUSTOM Minecraft-time calendar does when the overworld clock moves backwards (e.g. {@code /time set day}). */
public enum TimeJumpPolicy {
    /**
     * Never move the calendar backwards: jump forward to the next occurrence of the new time of day.
     * This matches how Serene Seasons treats small backward jumps. Default.
     */
    FORWARD_TO_NEXT_TIME_OF_DAY,
    /** Keep the current calendar day and only change the time of day (may repeat part of a day). */
    KEEP_DATE,
    /** Rewind the calendar by the same amount the clock moved back (never before Year 1, January 1). */
    REWIND
}
