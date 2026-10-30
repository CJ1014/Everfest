package com.holidayseasons.holiday;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge game bus (server thread) for every holiday lifecycle transition, in order.
 * Listeners must be idempotent per {@link HolidayTransition#occurrenceKey()}: after a crash the same transition may be
 * delivered again because occurrence records are saved together with the world.
 */
public final class HolidayPhaseChangedEvent extends Event {
    private final MinecraftServer server;
    private final ScheduledHoliday holiday;
    private final HolidayTransition transition;

    public HolidayPhaseChangedEvent(MinecraftServer server, ScheduledHoliday holiday, HolidayTransition transition) {
        this.server = server;
        this.holiday = holiday;
        this.transition = transition;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ScheduledHoliday getHoliday() {
        return holiday;
    }

    public HolidayTransition getTransition() {
        return transition;
    }
}
