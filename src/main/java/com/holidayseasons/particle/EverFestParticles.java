package com.holidayseasons.particle;

import com.holidayseasons.EverFest;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** EverFest's particle types (rendering is registered on the client). */
public final class EverFestParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, EverFest.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AUTUMN_LEAF = TYPES.register("autumn_leaf", () -> new SimpleParticleType(false));

    private EverFestParticles() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
