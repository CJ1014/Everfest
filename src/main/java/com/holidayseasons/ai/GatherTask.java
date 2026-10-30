package com.holidayseasons.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;

/** Walk to the festival square and celebrate there: look around, dance (hop) and hum (note particles). */
public final class GatherTask extends CelebrationTask {
    private final BlockPos centre;
    private final BlockPos standAt;
    private final int celebrateTicks;
    private int celebrated;

    public GatherTask(String occ, String village, BlockPos centre, BlockPos standAt, int celebrateTicks) {
        super(TaskType.GATHER, occ, village);
        this.centre = centre;
        this.standAt = standAt;
        this.celebrateTicks = celebrateTicks;
    }

    @Override
    protected int maxTicks() {
        return 900 + celebrateTicks;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (celebrated == 0 && !walkTo(v, standAt, 0.5f, 1)) return;
        celebrated++;
        lookAt(v, centre);
        if (celebrated % 30 == 0) {
            hop(v);
            particles(level, head(v), ParticleTypes.NOTE, 1, 0.2);
        }
        if (celebrated % 90 == 45) sound(level, v, level.random.nextBoolean() ? SoundEvents.VILLAGER_CELEBRATE : SoundEvents.VILLAGER_YES, 0.6f,
                0.9f + level.random.nextFloat() * 0.3f);
        if (celebrated >= celebrateTicks) finish();
    }
}
