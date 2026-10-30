package com.holidayseasons.client;

import com.holidayseasons.EverFest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point. */
@Mod(value = EverFest.MOD_ID, dist = Dist.CLIENT)
public final class EverFestClient {
    public EverFestClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(EverFest.id("date_hud"), new DateHudLayer()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientCalendarState.clear();
            ClientHolidayState.clear();
        });
    }
}
