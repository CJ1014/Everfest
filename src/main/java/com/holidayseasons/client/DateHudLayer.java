package com.holidayseasons.client;

import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/** Optional compact date read-out in the top-left corner. */
public final class DateHudLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.INSTANCE.showDateHud.get() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        CalendarSnapshot s = ClientCalendarState.snapshot();
        if (s == null) return;
        Component line = CalendarText.shortDate(s).copy().append(" · ").append(CalendarText.time(ClientCalendarState.liveTimeOfDay()));
        int w = mc.font.width(line);
        g.fill(2, 2, 8 + w, 14, 0x80000000);
        g.drawString(mc.font, line, 5, 4, 0xFFF2E6C9, false);
    }
}
