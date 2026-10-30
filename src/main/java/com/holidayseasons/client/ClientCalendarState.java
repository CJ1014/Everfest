package com.holidayseasons.client;

import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.ClockMath;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/** The latest calendar state received from the server. Cleared on disconnect. */
public final class ClientCalendarState {
    @Nullable
    private static CalendarSnapshot snapshot;
    private static long receivedAtMillis;

    private ClientCalendarState() {
    }

    public static void update(CalendarSnapshot s) {
        snapshot = s;
        receivedAtMillis = System.currentTimeMillis();
    }

    public static void clear() {
        snapshot = null;
    }

    @Nullable
    public static CalendarSnapshot snapshot() {
        return snapshot;
    }

    /** Time of day, kept smooth between syncs: Minecraft-time modes follow the client clock, real-world mode the wall clock. */
    public static int liveTimeOfDay() {
        CalendarSnapshot s = snapshot;
        if (s == null) return 0;
        if (s.mode() == CalendarMode.REAL_WORLD) {
            long elapsedTicks = (System.currentTimeMillis() - receivedAtMillis) * 24000L / 86_400_000L;
            return (int) Math.floorMod(s.timeOfDay() + elapsedTicks, ClockMath.TICKS_PER_DAY);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return (int) Math.floorMod(mc.level.getDayTime(), ClockMath.TICKS_PER_DAY);
        return s.timeOfDay();
    }
}
