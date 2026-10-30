package com.holidayseasons.client;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.ClockMath;
import com.holidayseasons.config.ClientConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Localised date and time formatting for client UI. */
public final class CalendarText {
    private CalendarText() {
    }

    public static Component monthName(int month) {
        return Component.translatable("holidayseasons.month." + month);
    }

    public static Component fullDate(CalendarDate d, CalendarMode mode) {
        String key = mode == CalendarMode.REAL_WORLD ? "holidayseasons.date.real" : "holidayseasons.date.game";
        return Component.translatable(key, monthName(d.month()), d.day(), d.year());
    }

    public static Component shortDate(CalendarSnapshot s) {
        return Component.translatable("holidayseasons.date.short", monthName(s.date().month()), s.date().day());
    }

    public static Component weekday(int isoDay) {
        return Component.translatable("holidayseasons.weekday." + isoDay);
    }

    public static Component time(int timeOfDay) {
        int h = ClockMath.hourOf(timeOfDay);
        int m = ClockMath.minuteOf(timeOfDay);
        if (ClientConfig.INSTANCE.use24HourClock.get()) {
            return Component.literal(String.format(Locale.ROOT, "%02d:%02d", h, m));
        }
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return Component.literal(String.format(Locale.ROOT, "%d:%02d %s", h12, m, h < 12 ? "AM" : "PM"));
    }
}
