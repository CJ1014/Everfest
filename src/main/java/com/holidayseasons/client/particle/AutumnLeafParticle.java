package com.holidayseasons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** A falling autumn leaf: vanilla's drifting-petal motion with EverFest's orange, red and yellow leaf sprites. */
public class AutumnLeafParticle extends CherryParticle {
    protected AutumnLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites);
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) -> new AutumnLeafParticle(level, x, y, z, sprites);
    }
}
