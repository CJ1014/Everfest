package com.holidayseasons.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/** Finding safe standing spots for creatures EverFest brings into the world. Never loads chunks. */
public final class SpawnSpots {
    private SpawnSpots() {
    }

    /** A standing spot within three blocks above or below {@code near}: sturdy floor, two blocks of air, no fluid. */
    @Nullable
    public static BlockPos groundNear(ServerLevel level, BlockPos near) {
        if (!level.isLoaded(near)) return null;
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos at = near.above(dy);
            if (level.isEmptyBlock(at) && level.isEmptyBlock(at.above()) && level.getFluidState(at).isEmpty()
                    && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
                return at;
            }
        }
        return null;
    }

    /** A standing spot between {@code minDist} and {@code maxDist} blocks (horizontally) from {@code centre}. */
    @Nullable
    public static BlockPos around(ServerLevel level, BlockPos centre, int minDist, int maxDist, RandomSource random, int tries) {
        for (int i = 0; i < tries; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));
            BlockPos at = groundNear(level, centre.offset(Mth.floor(Math.cos(angle) * dist), 0, Mth.floor(Math.sin(angle) * dist)));
            if (at != null) return at;
        }
        return null;
    }
}
