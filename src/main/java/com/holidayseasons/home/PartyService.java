package com.holidayseasons.home;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.ai.CelebrationTask;
import com.holidayseasons.ai.PartyGuestTask;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.economy.HolidayTokens;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Holiday parties hosted from a Holiday Hearth. While a holiday is being celebrated, a host invites the villagers
 * living nearby; whether each comes depends on the host's reputation in its village ({@link PartyRules}). Guests walk
 * over by themselves and never cross unloaded chunks (only loaded villagers are invited, and an unreachable home makes
 * them give up). When the party ends the host earns reputation in each guest's village and, with tokens enabled, a
 * festival token: once per holiday, however many parties are thrown.
 */
public final class PartyService {
    public static final class Party {
        public final UUID homeId;
        public final UUID host;
        public final String occurrence;
        public final ServerLevel level;
        public final BlockPos spot;
        public final long endsAt;
        final Map<UUID, PartyGuestTask> invited = new LinkedHashMap<>();
        final Map<UUID, String> arrived = new LinkedHashMap<>();

        Party(UUID homeId, UUID host, String occurrence, ServerLevel level, BlockPos spot, long endsAt) {
            this.homeId = homeId;
            this.host = host;
            this.occurrence = occurrence;
            this.level = level;
            this.spot = spot;
            this.endsAt = endsAt;
        }

        public int invitedCount() {
            return invited.size();
        }

        public int arrivedCount() {
            return arrived.size();
        }
    }

    private final EverFestServer services;
    private final Map<UUID, Party> byHome = new HashMap<>();
    private final Random random = new Random();

    public PartyService(EverFestServer services) {
        this.services = services;
    }

    @Nullable
    public Party partyAt(UUID homeId) {
        return byHome.get(homeId);
    }

    /** Starts a party at {@code home}. Returns null on success, or the translation key of the reason it could not start. */
    @Nullable
    public String host(ServerPlayer player, HomeRecord home) {
        if (ServerConfig.SPEC.isLoaded() && !ServerConfig.INSTANCE.hearthParties.get()) return "holidayseasons.party.disabled";
        CelebrationDirector.Festival f = services.director().festival();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return "holidayseasons.party.no_holiday";
        if (byHome.containsKey(home.id)) return "holidayseasons.party.already";
        ServerLevel level = services.server().getLevel(home.dimension);
        if (level == null) return "holidayseasons.party.no_holiday";
        int max = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.maxPartyGuests.get() : 6;
        int radius = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.partyInviteRadius.get() : 48;
        int duration = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.partyDurationTicks.get() : 4800;
        long now = level.getGameTime();
        Party party = new Party(home.id, player.getUUID(), f.occurrence().key(), level, home.entrance(), now + duration);
        var food = CelebrationDirector.feastFood(f);
        for (Villager v : services.director().villagersNear(level, home.hearth, radius)) {
            if (party.invited.size() >= max) break;
            if (v.isSleeping() || v.isTrading()) continue;
            CelebrationTask current = services.director().taskFor(v);
            if (current != null && current.isRunning()) continue;
            String village = services.director().resolveVillage(v).key();
            if (!PartyRules.accepts(services.reputation().get(player.getUUID(), village), random.nextDouble())) continue;
            PartyGuestTask task = new PartyGuestTask(f.occurrence().key(), village, home.id, home.entrance(), now, party.endsAt, food);
            services.director().assign(v, task);
            party.invited.put(v.getUUID(), task);
        }
        if (party.invited.isEmpty()) return "holidayseasons.party.nobody";
        byHome.put(home.id, party);
        player.displayClientMessage(Component.translatable("holidayseasons.party.started", party.invited.size()), false);
        EverFest.LOGGER.info("EverFest: {} hosts a party at {} with {} invited", player.getGameProfile().getName(), home.entrance(), party.invited.size());
        return null;
    }

    public void guestArrived(UUID homeId, Villager v) {
        Party p = byHome.get(homeId);
        if (p == null || p.arrived.containsKey(v.getUUID())) return;
        p.arrived.put(v.getUUID(), services.director().resolveVillage(v).key());
        ServerPlayer host = services.server().getPlayerList().getPlayer(p.host);
        if (host != null) host.displayClientMessage(Component.translatable("holidayseasons.party.arrived", v.getName(), p.arrived.size()), true);
    }

    public void tick() {
        if (byHome.isEmpty()) return;
        long now = services.server().overworld().getGameTime();
        if (now % 20 != 0) return;
        CelebrationDirector.Festival f = services.director().festival();
        boolean celebrating = f != null && f.phase() == HolidayPhase.ACTIVE;
        byHome.values().removeIf(p -> {
            // A party ends on time, or as soon as its holiday is no longer being celebrated.
            boolean sameHoliday = celebrating && f.occurrence().key().equals(p.occurrence);
            if (sameHoliday && p.level.getGameTime() < p.endsAt) return false;
            end(p);
            return true;
        });
    }

    /** Ends a party now (also used by tests). */
    public void endNow(UUID homeId) {
        Party p = byHome.remove(homeId);
        if (p != null) end(p);
    }

    private void end(Party p) {
        for (Map.Entry<UUID, PartyGuestTask> e : p.invited.entrySet()) {
            if (p.level.getEntity(e.getKey()) instanceof Villager v) services.director().cancel(v, e.getValue());
        }
        ServerPlayer host = services.server().getPlayerList().getPlayer(p.host);
        if (host != null) host.displayClientMessage(Component.translatable("holidayseasons.party.over", p.arrived.size(), p.invited.size()), false);
        if (p.arrived.isEmpty() || !services.gifts().claims().claim("party|" + p.occurrence + "|" + p.host)) return;
        long day = services.calendar().snapshot().epochDay();
        Set<String> villages = new HashSet<>(p.arrived.values());
        for (String village : villages) services.reputation().add(p.host, village, ReputationMath.Action.FESTIVAL_PARTICIPATION, day);
        if (host != null) HolidayTokens.award(host, 1, "holidayseasons.tokens.reason.party");
    }
}
