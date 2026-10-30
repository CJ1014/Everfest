package com.holidayseasons.holiday;

/** Lifecycle of one holiday occurrence. Phases only move forward for a given occurrence. */
public enum HolidayPhase {
    INACTIVE, PREPARING, ACTIVE, CLEANUP, COMPLETED;

    public boolean isCelebrating() {
        return this == PREPARING || this == ACTIVE;
    }

    public boolean isLive() {
        return this == PREPARING || this == ACTIVE || this == CLEANUP;
    }
}
