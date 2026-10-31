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
        r.playToClient(OpenHearthPayload.TYPE, OpenHearthPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onOpenHearth(p, ctx));
        r.playToServer(UpdateHearthPayload.TYPE, UpdateHearthPayload.STREAM_CODEC, UpdateHearthPayload::handle);
        r.playToServer(HostPartyPayload.TYPE, HostPartyPayload.STREAM_CODEC, HostPartyPayload::handle);
        r.playToServer(SetConfigPayload.TYPE, SetConfigPayload.STREAM_CODEC, SetConfigPayload::handle);
        r.playToClient(AdminConfigPayload.TYPE, AdminConfigPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onAdminConfig(p, ctx));
        r.playToClient(CollectionPayload.TYPE, CollectionPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onCollection(p, ctx));
        r.playToClient(OpenWrapPayload.TYPE, OpenWrapPayload.STREAM_CODEC,
                (p, ctx) -> com.holidayseasons.client.ClientPayloadHandlers.onOpenWrap(p, ctx));
        r.playToServer(WrapPresentPayload.TYPE, WrapPresentPayload.STREAM_CODEC, WrapPresentPayload::handle);
    }
}
