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
        com.holidayseasons.villager.HolidaySchedules.register(modBus);
        com.holidayseasons.block.entity.EverFestBlockEntities.register(modBus);
        com.holidayseasons.entity.EverFestEntities.register(modBus);
        com.holidayseasons.sound.EverFestSounds.register(modBus);
        com.holidayseasons.item.EverFestDataComponents.register(modBus);
        com.holidayseasons.worldgen.EverFestWorldgen.register(modBus);
        com.holidayseasons.particle.EverFestParticles.register(modBus);

        modBus.addListener(EverFestNetwork::register);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onConfigReload);
        modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> {
            e.register(com.holidayseasons.gametest.CalendarGameTests.class);
            e.register(com.holidayseasons.gametest.VillageGameTests.class);
            e.register(com.holidayseasons.gametest.HalloweenGameTests.class);
            e.register(com.holidayseasons.gametest.ChristmasGameTests.class);
            e.register(com.holidayseasons.gametest.SpringGameTests.class);
            e.register(com.holidayseasons.gametest.WildlifeGameTests.class);
            e.register(com.holidayseasons.gametest.WorldGameTests.class);
            e.register(com.holidayseasons.gametest.EconomyGameTests.class);
            e.register(com.holidayseasons.gametest.FestivalGameTests.class);
        });

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
        NeoForge.EVENT_BUS.addListener((com.holidayseasons.holiday.HolidayPhaseChangedEvent e) -> {
            EverFestServer s = EverFestServer.get();
            if (s != null) s.director().onPhaseChanged(e);
        });
        NeoForge.EVENT_BUS.addListener(this::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onUseBlock);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
            EverFestServer s = EverFestServer.get();
            if (s != null && !e.getLevel().isClientSide() && e.getEntity() instanceof net.minecraft.world.entity.npc.Villager v) {
                s.director().untrack(v);
            }
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

    private void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        EverFestServer s = EverFestServer.get();
        if (s == null) return;
        if (event.getEntity() instanceof net.minecraft.world.entity.npc.Villager v) {
            s.director().track(v);
        } else if (s.ledger().shouldRejectEntity(event.getEntity(), s.calendar().schedulingSnapshot().epochDay())
                || s.halloween().shouldRejectCreature(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Players hand candy to children, give presents, find Easter eggs and hear villagers' Christmas wishes. */
    private void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        EverFestServer s = EverFestServer.get();
        if (s == null) return;
        if (event.getTarget() instanceof net.minecraft.world.entity.Interaction hitbox && com.holidayseasons.event.EasterService.isEgg(hitbox)) {
            s.easter().collect(event.getEntity(), hitbox);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        if (!(event.getTarget() instanceof net.minecraft.world.entity.npc.Villager child)) return;
        net.minecraft.world.item.ItemStack held = event.getItemStack();
        if (s.gifts().giveValentine(event.getEntity(), child, held)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        if (held.getItem() instanceof com.holidayseasons.item.PresentItem) {
            if (s.gifts().giveToVillager(event.getEntity(), child, held) >= 0) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
            return;
        }
        if (held.isEmpty() && event.getEntity().isShiftKeyDown() && s.gifts().tellWishes(event.getEntity(), child)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        if (!child.isBaby() || !held.is(com.holidayseasons.item.EverFestItems.CANDY_TAG)) return;
        if (s != null && s.halloween().playerGivesCandy(event.getEntity(), child, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    /** Village objectives: festive goods are handed in by using them on the village bell. */
    private void onUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty()) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.BELL)) return;
        EverFestServer s = EverFestServer.get();
        if (s == null || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (s.objectives().contribute(level, event.getEntity(), event.getPos(), event.getItemStack()) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    /** Anti-grief: only the owner (or an operator) may break a Holiday Hearth. */
    private void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.getState().is(com.holidayseasons.block.EverFestBlocks.HOLIDAY_HEARTH.get())) return;
        if (ServerConfig.SPEC.isLoaded() && !ServerConfig.INSTANCE.protectHearths.get()) return;
        EverFestServer s = EverFestServer.get();
        if (s == null || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var home = s.homes().atHearth(level.dimension(), event.getPos());
        var player = event.getPlayer();
        if (home != null && !home.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("holidayseasons.home.protected", home.ownerName()), true);
        }
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EverFestServer s = EverFestServer.get();
        if (s != null && event.getEntity() instanceof ServerPlayer player) {
            s.calendar().sendTo(player);
            s.holidays().sendTo(player);
        }
    }
}
