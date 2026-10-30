package com.holidayseasons.client;

import com.holidayseasons.holiday.HolidayStatus;

import java.util.List;

/** Holiday statuses most recently received from the server, sorted by start date. */
public final class ClientHolidayState {
    private static List<HolidayStatus> statuses = List.of();

    private ClientHolidayState() {
    }

    public static void update(List<HolidayStatus> list) {
        statuses = List.copyOf(list);
    }

    public static void clear() {
        statuses = List.of();
    }

    public static List<HolidayStatus> statuses() {
        return statuses;
    }
}
