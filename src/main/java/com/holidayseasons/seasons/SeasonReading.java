package com.holidayseasons.seasons;

import com.holidayseasons.calendar.SubSeason;

/**
 * A snapshot of an external season provider's state, in EverFest terms.
 *
 * @param cycleTicks     ticks into the provider's seasonal cycle (0 = start of early spring)
 * @param subSeasonTicks ticks per sub-season
 * @param dayDuration    ticks per provider day
 * @param subSeason      current sub-season
 * @param cycling        false if the provider's seasons are currently frozen (e.g. doSeasonCycle=false)
 */
public record SeasonReading(long cycleTicks, long subSeasonTicks, long dayDuration, SubSeason subSeason, boolean cycling) {
    public long cycleLength() {
        return subSeasonTicks * 12L;
    }

    public int daysPerSubSeason() {
        return (int) Math.max(1L, subSeasonTicks / Math.max(1L, dayDuration));
    }
}
