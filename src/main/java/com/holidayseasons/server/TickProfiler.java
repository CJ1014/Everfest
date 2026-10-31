package com.holidayseasons.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Measures how long each EverFest system takes per server tick (wall-clock nanoseconds, averaged since the last
 * reset). Always on: two {@code System.nanoTime()} calls per system per tick cost far less than a microsecond.
 */
public final class TickProfiler {
    private final Map<String, long[]> totals = new LinkedHashMap<>();
    private long ticks;
    private long worstTick;
    private long tickStart;
    private long sectionStart;

    public void beginTick() {
        tickStart = System.nanoTime();
        sectionStart = tickStart;
    }

    /** Ends the section that started at the previous mark. */
    public void mark(String section) {
        long now = System.nanoTime();
        long[] t = totals.computeIfAbsent(section, k -> new long[2]);
        long d = now - sectionStart;
        t[0] += d;
        t[1] = Math.max(t[1], d);
        sectionStart = now;
    }

    public void endTick() {
        ticks++;
        worstTick = Math.max(worstTick, System.nanoTime() - tickStart);
    }

    public void reset() {
        totals.clear();
        ticks = 0;
        worstTick = 0;
    }

    public long ticks() {
        return ticks;
    }

    /** Average microseconds per tick of all EverFest work. */
    public double averageMicros() {
        if (ticks == 0) return 0;
        long sum = 0;
        for (long[] t : totals.values()) sum += t[0];
        return sum / 1000.0 / ticks;
    }

    public List<String> report() {
        List<String> out = new ArrayList<>();
        if (ticks == 0) return out;
        out.add(String.format(Locale.ROOT, "EverFest over %d ticks: %.1f us/tick on average (worst tick %.2f ms; a tick may take 50 ms)",
                ticks, averageMicros(), worstTick / 1e6));
        totals.forEach((name, t) -> out.add(String.format(Locale.ROOT, "  %-11s %7.1f us/tick, worst %.2f ms", name, t[0] / 1000.0 / ticks, t[1] / 1e6)));
        return out;
    }
}
