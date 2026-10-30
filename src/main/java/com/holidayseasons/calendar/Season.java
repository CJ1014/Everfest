package com.holidayseasons.calendar;

import java.util.Locale;

/** EverFest's own season model. Mirrors Serene Seasons' four seasons without depending on it. */
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Season byId(String id) {
        for (Season s : values()) if (s.id().equalsIgnoreCase(id)) return s;
        throw new IllegalArgumentException("Unknown season: " + id);
    }
}
