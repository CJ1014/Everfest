package com.holidayseasons.calendar;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent calendar clock state, stored in the overworld's data folder as {@code holidayseasons_calendar.dat}.
 * The world's day-time is never used directly as a date because {@code /time set} rewrites it absolutely.
 */
public final class CalendarClockData extends SavedData {
    public static final String FILE_ID = "holidayseasons_calendar";
    private static final int DATA_VERSION = 1;

    public static final Factory<CalendarClockData> FACTORY = new Factory<>(CalendarClockData::new, CalendarClockData::load, null);

    /** Calendar ticks elapsed since the world's calendar began. Follows the overworld clock with the jump policy. */
    long calendarTicks;
    /** Overworld day-time observed last tick; {@link Long#MIN_VALUE} until first observed. */
    long lastDayTime = Long.MIN_VALUE;
    /** Admin day adjustments for the CUSTOM and SEASON_SYNCED-fallback calendars. */
    long customDayOffset;
    long fallbackDayOffset;
    /** Year counter for the Serene Seasons cycle (Serene Seasons itself does not count years). */
    int seasonYear = 1;
    long lastSeasonYearTicks = -1;
    /** Last date shown while synced to Serene Seasons; lets the fallback calendar continue from it if SS is removed. */
    @Nullable
    String lastSyncedDate;
    /** REAL_WORLD admin overrides. */
    @Nullable
    String realDateOverride;
    long realDayOffset;

    public CalendarClockData() {
    }

    public static CalendarClockData load(CompoundTag tag, HolderLookup.Provider registries) {
        CalendarClockData d = new CalendarClockData();
        d.calendarTicks = tag.getLong("calendarTicks");
        d.lastDayTime = tag.contains("lastDayTime") ? tag.getLong("lastDayTime") : Long.MIN_VALUE;
        d.customDayOffset = tag.getLong("customDayOffset");
        d.fallbackDayOffset = tag.getLong("fallbackDayOffset");
        d.seasonYear = Math.max(1, tag.getInt("seasonYear"));
        d.lastSeasonYearTicks = tag.contains("lastSeasonYearTicks") ? tag.getLong("lastSeasonYearTicks") : -1;
        d.realDateOverride = tag.contains("realDateOverride") ? tag.getString("realDateOverride") : null;
        d.lastSyncedDate = tag.contains("lastSyncedDate") ? tag.getString("lastSyncedDate") : null;
        d.realDayOffset = tag.getLong("realDayOffset");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", DATA_VERSION);
        tag.putLong("calendarTicks", calendarTicks);
        if (lastDayTime != Long.MIN_VALUE) tag.putLong("lastDayTime", lastDayTime);
        tag.putLong("customDayOffset", customDayOffset);
        tag.putLong("fallbackDayOffset", fallbackDayOffset);
        tag.putInt("seasonYear", seasonYear);
        tag.putLong("lastSeasonYearTicks", lastSeasonYearTicks);
        if (realDateOverride != null) tag.putString("realDateOverride", realDateOverride);
        if (lastSyncedDate != null) tag.putString("lastSyncedDate", lastSyncedDate);
        tag.putLong("realDayOffset", realDayOffset);
        return tag;
    }

    public long calendarTicks() {
        return calendarTicks;
    }

    public int seasonYear() {
        return seasonYear;
    }

    @Nullable
    public String realDateOverride() {
        return realDateOverride;
    }

    public long realDayOffset() {
        return realDayOffset;
    }
}
