package com.holidayseasons.decoration;

/** Pure arithmetic for how many decorations a village should have and when each one comes down. */
public final class DecorationSchedule {
    private DecorationSchedule() {
    }

    /**
     * Decorations a village should have up on a given day: ramps up over the preparation days, full while active.
     *
     * @param prepDayIndex 0-based day within preparation (ignored when active)
     */
    public static int target(int maxPerVillage, double intensity, boolean active, int prepDayIndex, int prepDays) {
        int max = (int) Math.round(maxPerVillage * Math.max(0, Math.min(2, intensity)));
        if (active || prepDays <= 0) return max;
        int day = Math.max(0, Math.min(prepDays - 1, prepDayIndex));
        return (int) Math.ceil(max * (day + 1) / (double) (prepDays + 1));
    }

    /**
     * Day on which a decoration placed for an occurrence is removed: spread over the cleanup days so decorations
     * disappear gradually. {@code seed} is any stable per-decoration number.
     */
    public static long expiryDay(long cleanupStartDay, int cleanupDays, long seed) {
        if (cleanupDays <= 0) return cleanupStartDay;
        return cleanupStartDay + 1 + Math.floorMod(seed * 0x9E3779B97F4A7C15L >>> 7, (long) cleanupDays);
    }
}
