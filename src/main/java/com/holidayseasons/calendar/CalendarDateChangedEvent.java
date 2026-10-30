package com.holidayseasons.calendar;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/** Posted on the NeoForge game bus (server side) whenever the calendar date, mode or sub-season changes. */
public final class CalendarDateChangedEvent extends Event {
    private final MinecraftServer server;
    @Nullable
    private final CalendarSnapshot previous;
    private final CalendarSnapshot current;

    public CalendarDateChangedEvent(MinecraftServer server, @Nullable CalendarSnapshot previous, CalendarSnapshot current) {
        this.server = server;
        this.previous = previous;
        this.current = current;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** Null on the first tick after the server starts. */
    @Nullable
    public CalendarSnapshot getPrevious() {
        return previous;
    }

    public CalendarSnapshot getCurrent() {
        return current;
    }
}
