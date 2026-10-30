package com.holidayseasons.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers EverFest payloads. Client-bound handlers delegate to {@code com.holidayseasons.client.ClientPayloadHandlers}
 * through lambdas so that class is never loaded on a dedicated server.
 */
public final class EverFestNetwork {
    public static final String PROTOCOL = "1";

    private EverFestNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);
        r.playToClient(CalendarSyncPayload.TYPE, CalendarSyncPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onCalendarSync(p, ctx));
        r.playToClient(OpenScreenPayload.TYPE, OpenScreenPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onOpenScreen(p, ctx));
        r.playToClient(HolidayStatusPayload.TYPE, HolidayStatusPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onHolidayStatus(p, ctx));
        r.playToClient(HolidayNoticePayload.TYPE, HolidayNoticePayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onHolidayNotice(p, ctx));
    }
}
