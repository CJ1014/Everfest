package com.holidayseasons.client;

import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.particle.EverFestParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Client-side seasonal particles around the player, scaled by the effects intensity: autumn leaves and spring petals
 * drifting from nearby foliage, and gentle winter flurries (not in hot biomes, nor while it is already snowing).
 */
public final class SeasonalAtmosphere {
    private SeasonalAtmosphere() {
    }

    static void tick(ClientLevel level, LocalPlayer player, CalendarSnapshot snap, float k, RandomSource r) {
        if (snap == null || !ClientConfig.INSTANCE.seasonalParticles.get() || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        Season season = snap.season();
        if (season == Season.AUTUMN || season == Season.SPRING) {
            ParticleOptions particle = season == Season.AUTUMN ? EverFestParticles.AUTUMN_LEAF.get() : ParticleTypes.CHERRY_LEAVES;
            for (int i = 0; i < (int) (3 * k); i++) {
                BlockPos p = player.blockPosition().offset(r.nextInt(25) - 12, r.nextInt(10) + 2, r.nextInt(25) - 12);
                if (level.getBlockState(p).is(BlockTags.LEAVES) && level.getBlockState(p.below()).isAir()) {
                    level.addParticle(particle, p.getX() + r.nextDouble(), p.getY() - 0.1, p.getZ() + r.nextDouble(), 0, 0, 0);
                }
            }
        } else if (season == Season.WINTER && !level.isRaining() && level.getBiome(player.blockPosition()).value().getBaseTemperature() < 1.0f) {
            // Gentle flurries, except in hot biomes (deserts, savannas, jungles, badlands).
            for (int i = 0; i < (int) (4 * k); i++) {
                int x = player.getBlockX() + r.nextInt(33) - 16;
                int z = player.getBlockZ() + r.nextInt(33) - 16;
                int sky = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                double y = Math.max(sky, player.getY()) + 4 + r.nextInt(8);
                level.addParticle(ParticleTypes.SNOWFLAKE, x + r.nextDouble(), y, z + r.nextDouble(),
                        (r.nextDouble() - 0.5) * 0.02, -0.03 - r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.02);
            }
        }
    }
}
