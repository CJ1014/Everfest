package com.holidayseasons.calendar;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Validated, immutable calendar settings. Built from the server config (or tests) through {@link #sanitize},
 * which never throws: invalid values are replaced by defaults and reported in {@link #problems()}.
 */
public record CalendarSettings(
        CalendarMode mode,
        TimeJumpPolicy timeJumpPolicy,
        int[] customMonthLengths,
        int customLeapInterval,
        int customLeapMonth,
        CalendarDate customStartDate,
        int referenceYearBase,
        int fallbackDaysPerMonth,
        ZoneId realWorldZone,
        SeasonSyncPolicy seasonSyncPolicy,
        List<String> problems
) {
    public static final int[] GREGORIAN_LENGTHS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public static CalendarSettings defaults() {
        return sanitize(CalendarMode.SEASON_SYNCED, TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY,
                toList(GREGORIAN_LENGTHS), 4, 2, "0001-03-01", 2000, 8, "system", SeasonSyncPolicy.FOLLOW_DATE);
    }

    public static CalendarSettings sanitize(CalendarMode mode, TimeJumpPolicy jumpPolicy, List<? extends Number> monthLengths,
                                            int leapInterval, int leapMonth, String startDate, int referenceYearBase,
                                            int fallbackDaysPerMonth, String timezone, SeasonSyncPolicy syncPolicy) {
        List<String> problems = new ArrayList<>();
        if (mode == null) {
            problems.add("calendar mode missing, using SEASON_SYNCED");
            mode = CalendarMode.SEASON_SYNCED;
        }
        if (jumpPolicy == null) jumpPolicy = TimeJumpPolicy.FORWARD_TO_NEXT_TIME_OF_DAY;
        if (syncPolicy == null) syncPolicy = SeasonSyncPolicy.FOLLOW_DATE;

        int[] lengths = GREGORIAN_LENGTHS.clone();
        if (monthLengths == null || monthLengths.size() != 12) {
            problems.add("customMonthLengths must contain exactly 12 values; using Gregorian lengths");
        } else {
            boolean ok = true;
            int[] candidate = new int[12];
            for (int i = 0; i < 12; i++) {
                Number n = monthLengths.get(i);
                int v = n == null ? -1 : n.intValue();
                if (v < 1 || v > 400) {
                    ok = false;
                    problems.add("month " + (i + 1) + " length " + v + " out of range 1..400");
                }
                candidate[i] = v;
            }
            if (ok) lengths = candidate;
            else problems.add("using Gregorian month lengths");
        }
        if (leapInterval < 0 || leapInterval > 1000) {
            problems.add("customLeapInterval out of range, disabling leap years");
            leapInterval = 0;
        }
        if (leapMonth < 1 || leapMonth > 12) {
            problems.add("customLeapMonth out of range, using February");
            leapMonth = 2;
        }
        if (fallbackDaysPerMonth < 1 || fallbackDaysPerMonth > 400) {
            problems.add("fallbackDaysPerMonth out of range, using 8");
            fallbackDaysPerMonth = 8;
        }
        FixedMonthCalendarSystem custom = new FixedMonthCalendarSystem(lengths, leapInterval, leapMonth, referenceYearBase);
        CalendarDate start;
        try {
            start = CalendarDate.parseIso(startDate == null ? "" : startDate);
            if (start.year() < 1 || !custom.isValid(start)) throw new IllegalArgumentException("not a valid custom date");
        } catch (RuntimeException e) {
            problems.add("customStartDate '" + startDate + "' invalid (" + e.getMessage() + "), using 0001-03-01");
            start = custom.clamp(1, 3, 1);
        }
        ZoneId zone;
        try {
            zone = timezone == null || timezone.isBlank() || timezone.equalsIgnoreCase("system")
                    ? ZoneId.systemDefault() : ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            problems.add("unknown timezone '" + timezone + "', using system default");
            zone = ZoneId.systemDefault();
        }
        return new CalendarSettings(mode, jumpPolicy, lengths, leapInterval, leapMonth, start, referenceYearBase,
                fallbackDaysPerMonth, zone, syncPolicy, List.copyOf(problems));
    }

    public FixedMonthCalendarSystem customSystem() {
        return new FixedMonthCalendarSystem(customMonthLengths, customLeapInterval, customLeapMonth, referenceYearBase);
    }

    public FixedMonthCalendarSystem seasonSystem(int daysPerSubSeason) {
        return FixedMonthCalendarSystem.uniform(Math.max(1, daysPerSubSeason), referenceYearBase);
    }

    /** Days per month a Serene Seasons sub-season should last to match this custom calendar's average month. */
    public int recommendedSubSeasonDays() {
        int total = 0;
        for (int len : customMonthLengths) total += len;
        return Math.max(1, Math.round(total / 12f));
    }

    public static List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }
}
