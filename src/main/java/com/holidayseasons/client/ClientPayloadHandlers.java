package com.holidayseasons.client;

import com.holidayseasons.client.screen.CalendarScreen;
import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.network.CalendarSyncPayload;
import com.holidayseasons.network.HolidayNoticePayload;
import com.holidayseasons.network.HolidayStatusPayload;
import com.holidayseasons.network.OpenScreenPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side packet handling. Only ever loaded on the physical client. */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void onCalendarSync(CalendarSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCalendarState.update(payload.snapshot()));
    }

    public static void onHolidayStatus(HolidayStatusPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHolidayState.update(payload.signature(), payload.statuses()));
    }

    private static final SystemToast.SystemToastId HOLIDAY_TOAST = new SystemToast.SystemToastId(6000L);

    public static void onHolidayNotice(HolidayNoticePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ClientConfig.INSTANCE.showHolidayToasts.get()) return;
            String key = com.holidayseasons.holiday.HolidayService.statusKey(payload.holidayId());
            Component title = Component.translatable(key).withColor(payload.color() & 0xFFFFFF);
            Component body = Component.translatable(payload.phase() == HolidayPhase.ACTIVE
                    ? "holidayseasons.toast.active" : "holidayseasons.toast.preparing");
            SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), HOLIDAY_TOAST, title, body);
        });
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
