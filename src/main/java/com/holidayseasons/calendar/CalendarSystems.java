package com.holidayseasons.calendar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rebuilds a {@link CalendarSystem} from its {@link CalendarSystem#signature()}, e.g. on the client. */
public final class CalendarSystems {
    private static final Pattern FIXED = Pattern.compile("fixed\\[([0-9,]+)]L(\\d+)M(\\d+)");

    private CalendarSystems() {
    }

    public static CalendarSystem fromSignature(String signature, int referenceYearBase) {
        if (GregorianCalendarSystem.INSTANCE.signature().equals(signature)) return GregorianCalendarSystem.INSTANCE;
        Matcher m = FIXED.matcher(signature);
        if (!m.matches()) throw new IllegalArgumentException("Unknown calendar signature: " + signature);
        String[] parts = m.group(1).split(",");
        int[] lengths = new int[parts.length];
        for (int i = 0; i < parts.length; i++) lengths[i] = Integer.parseInt(parts[i]);
        return new FixedMonthCalendarSystem(lengths, Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), referenceYearBase);
    }
}
