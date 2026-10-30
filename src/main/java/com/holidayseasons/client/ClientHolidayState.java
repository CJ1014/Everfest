package com.holidayseasons.client;

import com.holidayseasons.holiday.HolidayStatus;

import java.util.List;

/** Holiday statuses most recently received from the server, sorted by start date. */
public final class ClientHolidayState {
    private static List<HolidayStatus> statuses = List.of();
    private static String signature = "";

    private ClientHolidayState() {
    }

    public static void update(String scheduleSignature, List<HolidayStatus> list) {
        signature = scheduleSignature;
        statuses = List.copyOf(list);
    }

    public static void clear() {
        statuses = List.of();
        signature = "";
    }

    /** Signature of the calendar the statuses' dates belong to (differs from the displayed one under FOLLOW_SEASON). */
    public static String signature() {
        return signature;
    }

    public static List<HolidayStatus> statuses() {
        return statuses;
    }
}
