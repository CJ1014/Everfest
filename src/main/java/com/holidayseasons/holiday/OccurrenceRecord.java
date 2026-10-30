package com.holidayseasons.holiday;

import net.minecraft.nbt.CompoundTag;

/**
 * Persisted state of one holiday occurrence (a holiday in one year of one calendar system).
 * Keys look like {@code holidayseasons:halloween|fixed[8,...]L0M2|3}.
 */
public final class OccurrenceRecord {
    public final String holidayId;
    public final String signature;
    public final int year;
    HolidayPhase phase = HolidayPhase.INACTIVE;
    String skipReason = "";
    long prepStart;
    long start;
    long end;
    long cleanupEnd;
    /** Number of times this occurrence entered ACTIVE. Must never exceed 1; asserted by tests. */
    int activations;

    public OccurrenceRecord(String holidayId, String signature, int year, HolidayWindow window) {
        this.holidayId = holidayId;
        this.signature = signature;
        this.year = year;
        setWindow(window);
    }

    public static String key(String holidayId, String signature, int year) {
        return holidayId + "|" + signature + "|" + year;
    }

    public String key() {
        return key(holidayId, signature, year);
    }

    void setWindow(HolidayWindow w) {
        prepStart = w.prepStart();
        start = w.start();
        end = w.end();
        cleanupEnd = w.cleanupEnd();
    }

    public HolidayWindow window() {
        return new HolidayWindow(prepStart, start, end, cleanupEnd);
    }

    public HolidayPhase phase() {
        return phase;
    }

    public String skipReason() {
        return skipReason;
    }

    public int activations() {
        return activations;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("holiday", holidayId);
        t.putString("signature", signature);
        t.putInt("year", year);
        t.putString("phase", phase.name());
        if (!skipReason.isEmpty()) t.putString("skip", skipReason);
        t.putLong("prepStart", prepStart);
        t.putLong("start", start);
        t.putLong("end", end);
        t.putLong("cleanupEnd", cleanupEnd);
        t.putInt("activations", activations);
        return t;
    }

    public static OccurrenceRecord load(CompoundTag t) {
        OccurrenceRecord r = new OccurrenceRecord(t.getString("holiday"), t.getString("signature"), t.getInt("year"),
                new HolidayWindow(t.getLong("prepStart"), t.getLong("start"), Math.max(t.getLong("start") + 1, t.getLong("end")),
                        Math.max(Math.max(t.getLong("start") + 1, t.getLong("end")), t.getLong("cleanupEnd"))));
        try {
            r.phase = HolidayPhase.valueOf(t.getString("phase"));
        } catch (IllegalArgumentException e) {
            r.phase = HolidayPhase.COMPLETED;
        }
        r.skipReason = t.getString("skip");
        r.activations = t.getInt("activations");
        return r;
    }
}
