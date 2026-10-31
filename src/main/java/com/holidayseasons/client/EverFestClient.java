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
    /** Present wrapping paper takes the colour chosen when wrapping (tint index 0). */
    private static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tint) -> {
            if (tint != 0 || level == null || pos == null) return -1;
            return level.getBlockEntity(pos) instanceof com.holidayseasons.block.entity.PresentBlockEntity be ? 0xFF000000 | be.info().color() : -1;
        }, com.holidayseasons.block.EverFestBlocks.PRESENT.get());
    }

    private static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> tint == 0 ? 0xFF000000 | com.holidayseasons.item.PresentItem.info(stack).color() : -1,
                com.holidayseasons.item.EverFestItems.PRESENT.get());
    }

    public EverFestClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(EverFest.id("date_hud"), new DateHudLayer()));
        modBus.addListener(com.holidayseasons.client.entity.HalloweenRenderers::registerLayers);
        modBus.addListener(EverFestClient::blockColors);
        modBus.addListener(EverFestClient::itemColors);
        modBus.addListener(com.holidayseasons.client.entity.HalloweenRenderers::registerRenderers);
        modBus.addListener(com.holidayseasons.client.entity.WildlifeRenderers::registerLayers);
        modBus.addListener(com.holidayseasons.client.entity.WildlifeRenderers::registerRenderers);
        modBus.addListener(com.holidayseasons.client.entity.WildlifeRenderers::addOutfitLayers);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) ->
                e.registerSpriteSet(com.holidayseasons.particle.EverFestParticles.AUTUMN_LEAF.get(),
                        com.holidayseasons.client.particle.AutumnLeafParticle::provider));
        NeoForge.EVENT_BUS.addListener(HolidayMusic::onSelectMusic);
        NeoForge.EVENT_BUS.addListener(HalloweenAtmosphere::onClientTick);
        NeoForge.EVENT_BUS.addListener(HalloweenAtmosphere::onRenderFog);
        NeoForge.EVENT_BUS.addListener(HalloweenAtmosphere::onFogColor);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientCalendarState.clear();
            ClientHolidayState.clear();
        });
    }
}
