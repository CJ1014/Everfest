package com.holidayseasons.client;

import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayStatus;
import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Holiday music: while a holiday is being celebrated, its original track takes the place of the overworld's usual
 * background music (menus, underwater, Nether and End music are left alone). Client option {@code holidayMusic}.
 */
public final class HolidayMusic {
    @Nullable
    private static Holder<SoundEvent> lastSelected;

    private HolidayMusic() {
    }

    private static void selected(@Nullable Holder<SoundEvent> track) {
        if (track == lastSelected) return;
        lastSelected = track;
        com.holidayseasons.EverFest.LOGGER.info("EverFest: holiday music {}", track == null ? "off" : track.value().getLocation());
    }

    @Nullable
    static Holder<SoundEvent> trackFor(String holidayId) {
        String path = holidayId.substring(holidayId.indexOf(':') + 1);
        return switch (path) {
            case "halloween" -> EverFestSounds.MUSIC_LANTERN_HOLLOW;
            case "christmas" -> EverFestSounds.MUSIC_HEARTHLIGHT;
            case "easter", "valentines" -> EverFestSounds.MUSIC_MEADOW_MORNING;
            case "thanksgiving" -> EverFestSounds.MUSIC_HARVEST_TABLE;
            case "new_years", "independence_day" -> EverFestSounds.MUSIC_MIDNIGHT_FIREWORKS;
            default -> null;
        };
    }

    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.level().dimension() != Level.OVERWORLD) return;
        if (!ClientConfig.SPEC.isLoaded() || !ClientConfig.INSTANCE.holidayMusic.get()) return;
        Music original = event.getOriginalMusic();
        if (original == Musics.UNDER_WATER || original == Musics.MENU || original == Musics.CREDITS) return;
        for (HolidayStatus s : ClientHolidayState.statuses()) {
            if (s.phase() != HolidayPhase.ACTIVE) continue;
            Holder<SoundEvent> track = trackFor(s.id());
            if (track != null) {
                event.setMusic(new Music(track, 1200, 6000, false));
                selected(track);
                return;
            }
        }
        selected(null);
    }
}
