package com.holidayseasons.ai;

import com.holidayseasons.event.HalloweenService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

/** An adult waits at its own front door (or the festival stall) with a bowl of candy for trick-or-treaters. */
public final class HandOutCandyTask extends CelebrationTask {
    private final HalloweenService service;
    private final BlockPos post;
    private final BlockPos facing;
    private final ItemStack candy;
    private final int duration;
    private int standing;

    public HandOutCandyTask(String occ, String village, HalloweenService service, BlockPos post, BlockPos facing, ItemStack candy, int duration) {
        super(TaskType.HAND_OUT_CANDY, occ, village);
        this.service = service;
        this.post = post;
        this.facing = facing;
        this.candy = candy;
        this.duration = duration;
    }

    public BlockPos post() {
        return post;
    }

    @Override
    protected int maxTicks() {
        return 1200 + duration;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (standing == 0 && !walkTo(v, post, 0.5f, 1)) return;
        if (standing == 0) {
            carry(v, candy);
            service.registerGiver(post, v);
        }
        standing++;
        lookAt(v, facing);
        if (standing % 80 == 0) particles(level, head(v), ParticleTypes.HAPPY_VILLAGER, 1, 0.2);
        if (standing >= duration) finish();
    }

    @Override
    protected void onEnd(ServerLevel level, Villager v) {
        service.unregisterGiver(post, v);
    }

    /** Called when a group is served by this villager. */
    public void serve(ServerLevel level, Villager v) {
        particles(level, head(v), ParticleTypes.HAPPY_VILLAGER, 5, 0.3);
        sound(level, v, net.minecraft.sounds.SoundEvents.VILLAGER_YES, 0.7f, 1.1f);
    }
}
