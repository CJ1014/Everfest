package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.village.VillageRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Night-time firework shows: the New Year's midnight countdown (titles 10..1, "Happy New Year!", fireworks over every
 * loaded village and around players) and the Independence Day dusk show. Each show runs once per occurrence, even
 * across restarts or time changes.
 */
public final class FireworkShowService {
    public static final String NEW_YEAR = EverFest.MOD_ID + ":new_years";
    public static final String INDEPENDENCE = EverFest.MOD_ID + ":independence_day";
    private final MinecraftServer server;
    private final EverFestServer services;
    private final Random random = new Random();
    private int showTicks = -1;
    private boolean countdown;

    public FireworkShowService(MinecraftServer server, EverFestServer services) {
        this.server = server;
        this.services = services;
    }

    public boolean running() {
        return showTicks >= 0;
    }

    /** Checked once a second; the New Year countdown starts at 17800 so its 200-tick count reaches "Happy New Year!" at midnight (18000). */
    public void tick() {
        ServerLevel level = server.overworld();
        if (showTicks >= 0) {
            runShow(level);
            return;
        }
        if (level.getGameTime() % 20 != 0) return;
        CelebrationDirector.Festival f = services.director().festival();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return;
        String id = f.holiday().id();
        long tod = level.getDayTime() % 24000L;
        boolean newYear = id.equals(NEW_YEAR) && tod >= 17800 && tod < 19000;
        boolean independence = id.equals(INDEPENDENCE) && tod >= 13000 && tod < 15000;
        if (!newYear && !independence) return;
        if (!services.gifts().claims().claim("show|" + f.occurrence().key())) return;
        start(newYear);
    }

    /** Starts a show now (also used by tests and operators). */
    public void start(boolean withCountdown) {
        countdown = withCountdown;
        showTicks = 0;
        EverFest.LOGGER.info("EverFest: firework show starting{}", withCountdown ? " with countdown" : "");
    }

    private void runShow(ServerLevel level) {
        int t = showTicks++;
        if (countdown && t <= 200) {
            if (t % 20 == 0) {
                int n = 10 - t / 20;
                Component title = n > 0 ? Component.literal(Integer.toString(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        : Component.translatable("holidayseasons.new_year.happy").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                for (ServerPlayer p : level.players()) {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(0, n > 0 ? 18 : 80, n > 0 ? 2 : 20));
                    p.connection.send(new ClientboundSetTitleTextPacket(title));
                    p.playNotifySound(n > 0 ? SoundEvents.NOTE_BLOCK_HAT.value() : SoundEvents.PLAYER_LEVELUP, SoundSource.AMBIENT, 1f, n > 0 ? 1f + (10 - n) * 0.05f : 1f);
                }
            }
            if (t < 200) return;
        }
        int showStart = countdown ? 200 : 0;
        int elapsed = t - showStart;
        if (elapsed % 8 == 0) {
            for (Vec3 at : launchPoints(level)) {
                Vec3 burst = at.add(random.nextGaussian() * 6, 18 + random.nextInt(10), random.nextGaussian() * 6);
                CelebrationDirector.burst(level, burst);
                if (elapsed % 24 == 0) level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 1, at.z, 20, 0.2, 4, 0.2, 0.1);
            }
        }
        if (elapsed > 600) showTicks = -1;
    }

    private List<Vec3> launchPoints(ServerLevel level) {
        List<Vec3> out = new ArrayList<>();
        for (VillageRecord r : services.director().villages().all()) {
            BlockPos c = r.centre();
            if (r.key.startsWith(level.dimension().location() + "|bell|") && level.isLoaded(c)) out.add(c.getCenter());
        }
        for (ServerPlayer p : level.players()) {
            boolean near = out.stream().anyMatch(v -> v.distanceToSqr(p.position()) < 64 * 64);
            if (!near && out.size() < 16) out.add(p.position().add(random.nextGaussian() * 20, 0, random.nextGaussian() * 20));
        }
        return out;
    }
}
