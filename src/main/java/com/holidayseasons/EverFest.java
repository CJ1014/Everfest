package com.holidayseasons;

import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.command.EverFestCommands;
import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.calendar.CalendarDateChangedEvent;
import com.holidayseasons.holiday.HolidayRegistry;
import com.holidayseasons.item.EverFestCreativeTabs;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.network.EverFestNetwork;
import com.holidayseasons.seasons.SeasonAdapters;
import com.holidayseasons.server.EverFestServer;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(EverFest.MOD_ID)
public final class EverFest {
    public static final String MOD_ID = "holidayseasons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EverFest(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        EverFestBlocks.register(modBus);
        EverFestItems.register(modBus);
        EverFestCreativeTabs.register(modBus);

        modBus.addListener(EverFestNetwork::register);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onConfigReload);
        modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> e.register(com.holidayseasons.gametest.CalendarGameTests.class));

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> EverFestCommands.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new HolidayRegistry()));
        NeoForge.EVENT_BUS.addListener((CalendarDateChangedEvent e) -> {
            EverFestServer s = EverFestServer.get();
            if (s != null) s.holidays().markDirty();
        });
        HolidayRegistry.onReload(() -> {
            EverFestServer s = EverFestServer.get();
            if (s != null) s.server().execute(() -> s.holidays().reloadDefinitions());
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SeasonAdapters::init);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            EverFestServer s = EverFestServer.get();
            if (s != null) s.server().execute(() -> {
                s.calendar().reloadSettings();
                s.holidays().reloadDefinitions();
            });
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        EverFestServer.start(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        EverFestServer.stop();
    }

    private void onServerTick(ServerTickEvent.Post event) {
        EverFestServer s = EverFestServer.get();
        if (s != null) s.tick();
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EverFestServer s = EverFestServer.get();
        if (s != null && event.getEntity() instanceof ServerPlayer player) {
            s.calendar().sendTo(player);
            s.holidays().sendTo(player);
        }
    }
}
