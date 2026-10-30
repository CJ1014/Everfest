package com.holidayseasons.client;

import com.holidayseasons.client.screen.CalendarScreen;
import com.holidayseasons.network.CalendarSyncPayload;
import com.holidayseasons.network.OpenScreenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side packet handling. Only ever loaded on the physical client. */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void onCalendarSync(CalendarSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCalendarState.update(payload.snapshot()));
    }

    public static void onOpenScreen(OpenScreenPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            switch (payload.screen()) {
                case CALENDAR -> mc.setScreen(new CalendarScreen());
                default -> {
                }
            }
        });
    }
}
