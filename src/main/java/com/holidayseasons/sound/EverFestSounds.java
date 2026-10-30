package com.holidayseasons.sound;

import com.holidayseasons.EverFest;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** EverFest's sound events. All audio files are original, synthesized by {@code tools/gen_sounds.py}. */
public final class EverFestSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, EverFest.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DOORBELL = reg("doorbell");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOCK = reg("knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANDY = reg("candy");
    public static final DeferredHolder<SoundEvent, SoundEvent> CANDY_BOWL_EMPTY = reg("candy_bowl_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_MOAN = reg("ghost_moan");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_HURT = reg("ghost_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_DEATH = reg("ghost_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DISTANT_BELL = reg("distant_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> EERIE_WIND = reg("eerie_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPERS = reg("whispers");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOURDLING_HOP = reg("gourdling_hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOURDLING_HURT = reg("gourdling_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOURDLING_DEATH = reg("gourdling_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCARECROW_RUSTLE = reg("scarecrow_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCARECROW_CREAK = reg("scarecrow_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRICK_POOF = reg("trick_poof");

    private EverFestSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(EverFest.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
