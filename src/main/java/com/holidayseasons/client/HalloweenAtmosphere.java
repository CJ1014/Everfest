package com.holidayseasons.client;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayStatus;
import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client-only seasonal atmosphere: autumn leaves, Halloween night fog with a violet tint, distant bells, wind and
 * whispers, and drifting wisps. Every effect is optional and scaled by the client's effects intensity.
 */
public final class HalloweenAtmosphere {
    private static float fogBlend;
    private static int nextAmbient = 200;

    private HalloweenAtmosphere() {
    }

    static boolean halloweenActive() {
        String id = EverFest.MOD_ID + ":halloween";
        for (HolidayStatus s : ClientHolidayState.statuses()) {
            if (s.id().equals(id) && s.phase() == HolidayPhase.ACTIVE && s.skippedReason().isEmpty()) return true;
        }
        return false;
    }

    static boolean isNight(ClientLevel level) {
        long t = level.getDayTime() % 24000L;
        return t >= 12800 && t < 23200;
    }

    private static float intensity() {
        return ClientConfig.INSTANCE.effectsIntensity.get().floatValue();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        boolean spooky = halloweenActive() && isNight(level) && level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
        float target = spooky && ClientConfig.INSTANCE.halloweenFog.get() ? 1f : 0f;
        fogBlend += (target - fogBlend) * 0.02f;
        float k = intensity();
        if (k <= 0) return;
        RandomSource r = level.random;
        CalendarSnapshot snap = ClientCalendarState.snapshot();
        boolean autumn = snap != null && snap.season() == Season.AUTUMN;
        if (autumn && ClientConfig.INSTANCE.seasonalParticles.get()) {
            for (int i = 0; i < (int) (3 * k); i++) {
                BlockPos p = player.blockPosition().offset(r.nextInt(25) - 12, r.nextInt(10) + 2, r.nextInt(25) - 12);
                if (level.getBlockState(p).is(net.minecraft.tags.BlockTags.LEAVES) && level.getBlockState(p.below()).isAir()) {
                    level.addParticle(ParticleTypes.CHERRY_LEAVES, p.getX() + r.nextDouble(), p.getY() - 0.1, p.getZ() + r.nextDouble(), 0, 0, 0);
                }
            }
        }
        if (!spooky) return;
        // Wisps drifting in dark places.
        if (r.nextFloat() < 0.15f * k) {
            BlockPos p = player.blockPosition().offset(r.nextInt(33) - 16, r.nextInt(6), r.nextInt(33) - 16);
            if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p) < 4 && level.getBlockState(p).isAir()) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0.005, 0);
            }
        }
        if (--nextAmbient <= 0 && ClientConfig.INSTANCE.ambientSounds.get()) {
            nextAmbient = 400 + r.nextInt(900);
            double a = r.nextDouble() * Math.PI * 2;
            double x = player.getX() + Math.cos(a) * 24, z = player.getZ() + Math.sin(a) * 24;
            int pick = r.nextInt(5);
            var sound = pick < 2 ? EverFestSounds.DISTANT_BELL.get() : pick < 4 ? EverFestSounds.EERIE_WIND.get() : EverFestSounds.WHISPERS.get();
            level.playLocalSound(x, player.getY() + 4, z, sound, SoundSource.AMBIENT, 0.6f * k, 0.9f + r.nextFloat() * 0.2f, false);
        }
    }

    /** Pulls the fog in on Halloween nights (never below a third of the render distance). */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (fogBlend < 0.01f || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float k = Math.min(1f, intensity());
        float far = event.getFarPlaneDistance();
        float spookyFar = Math.max(far * 0.35f, 28f);
        event.setFarPlaneDistance(Mth.lerp(fogBlend * k, far, spookyFar));
        event.setNearPlaneDistance(Mth.lerp(fogBlend * k, event.getNearPlaneDistance(), 0f));
        event.setCanceled(true);
    }

    /** A faint violet-green tint for the night fog. */
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (fogBlend < 0.01f) return;
        float b = fogBlend * Math.min(1f, intensity()) * 0.6f;
        event.setRed(Mth.lerp(b, event.getRed(), 0.16f));
        event.setGreen(Mth.lerp(b, event.getGreen(), 0.12f));
        event.setBlue(Mth.lerp(b, event.getBlue(), 0.24f));
    }
}
