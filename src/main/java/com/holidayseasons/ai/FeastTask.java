package com.holidayseasons.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

/** Gather at the feast and share a meal: hold the dish and take bites. */
public final class FeastTask extends CelebrationTask {
    private final BlockPos table;
    private final BlockPos seat;
    private final ItemStack food;
    private int eating;

    public FeastTask(String occ, String village, BlockPos table, BlockPos seat, ItemStack food) {
        super(TaskType.FEAST, occ, village);
        this.table = table;
        this.seat = seat;
        this.food = food;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (eating == 0 && !walkTo(v, seat, 0.5f, 1)) return;
        if (eating == 0) carry(v, food);
        eating++;
        lookAt(v, table);
        if (eating % 16 == 0 && eating <= 16 * 6) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food), v.getX(), v.getEyeY() - 0.1, v.getZ(), 6, 0.15, 0.1, 0.15, 0.05);
            sound(level, v, SoundEvents.GENERIC_EAT, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        if (eating == 16 * 7) {
            dropCarried(v);
            sound(level, v, SoundEvents.VILLAGER_YES, 0.7f, 1.1f);
            particles(level, head(v), ParticleTypes.HAPPY_VILLAGER, 4, 0.3);
        }
        if (eating > 16 * 9) finish();
    }
}
