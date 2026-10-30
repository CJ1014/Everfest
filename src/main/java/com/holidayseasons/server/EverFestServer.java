package com.holidayseasons.server;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarService;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the per-server EverFest services. Created when a server starts and discarded when it stops, so an integrated
 * server reopened in the same client session never sees stale state.
 */
public final class EverFestServer {
    @Nullable
    private static EverFestServer instance;

    private final MinecraftServer server;
    private final CalendarService calendar;

    private EverFestServer(MinecraftServer server) {
        this.server = server;
        this.calendar = new CalendarService(server);
    }

    public static void start(MinecraftServer server) {
        instance = new EverFestServer(server);
        EverFest.LOGGER.info("EverFest: services started, today is {}", instance.calendar.snapshot().date());
    }

    public static void stop() {
        instance = null;
    }

    @Nullable
    public static EverFestServer get() {
        return instance;
    }

    public static EverFestServer require() {
        if (instance == null) throw new IllegalStateException("EverFest server services are not running");
        return instance;
    }

    public MinecraftServer server() {
        return server;
    }

    public CalendarService calendar() {
        return calendar;
    }

    public void tick() {
        calendar.tick();
    }
}
