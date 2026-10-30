package com.holidayseasons.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A multi-step holiday activity driven through vanilla brain memories (walk and look targets), so vanilla
 * pathfinding moves the villager. Subclasses implement {@link #onTick}; helpers keep the visible behaviour
 * consistent (carrying items, hopping, particles, sounds).
 */
public abstract class CelebrationTask {
    public final TaskType type;
    public final String occurrenceKey;
    public final String villageKey;
    protected int age;
    private boolean done;
    private boolean failed;
    private boolean running;
    private ItemStack carried = ItemStack.EMPTY;
    private BlockPos lastPos;
    private int stuckTicks;

    protected CelebrationTask(TaskType type, String occurrenceKey, String villageKey) {
        this.type = type;
        this.occurrenceKey = occurrenceKey;
        this.villageKey = villageKey;
    }

    /** Hard limit in ticks; the task ends (as failed) after this. */
    protected int maxTicks() {
        return 1200;
    }

    protected void onStart(ServerLevel level, Villager v) {
    }

    protected abstract void onTick(ServerLevel level, Villager v);

    protected void onEnd(ServerLevel level, Villager v) {
    }

    final void begin(ServerLevel level, Villager v) {
        running = true;
        lastPos = v.blockPosition();
        onStart(level, v);
    }

    final void tickTask(ServerLevel level, Villager v) {
        if (done) return;
        if (++age > maxTicks()) {
            fail();
            return;
        }
        onTick(level, v);
    }

    final void end(ServerLevel level, Villager v) {
        running = false;
        onEnd(level, v);
        dropCarried(v);
    }

    public boolean isDone() {
        return done;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean failed() {
        return failed;
    }

    public void finish() {
        done = true;
    }

    public void fail() {
        failed = true;
        done = true;
    }

    // ------------------------------------------------------------------ helpers

    /** Walks towards {@code target}; returns true once within {@code closeEnough} blocks. Fails if unreachable/stuck. */
    protected boolean walkTo(Villager v, BlockPos target, float speed, int closeEnough) {
        double distSq = v.blockPosition().distSqr(target);
        if (distSq <= (double) (closeEnough + 0.5) * (closeEnough + 0.5)) {
            v.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return true;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(v, target, speed, closeEnough);
        if (unreachable(v)) fail();
        trackStuck(v);
        return false;
    }

    protected boolean walkTo(Villager v, Entity target, float speed, int closeEnough) {
        if (v.distanceToSqr(target) <= (closeEnough + 0.5) * (closeEnough + 0.5)) {
            v.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return true;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(v, target, speed, closeEnough);
        if (unreachable(v)) fail();
        trackStuck(v);
        return false;
    }

    private boolean unreachable(Villager v) {
        return v.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                .map(since -> v.level().getGameTime() - since > 100).orElse(false);
    }

    private void trackStuck(Villager v) {
        BlockPos p = v.blockPosition();
        if (p.equals(lastPos)) {
            if (++stuckTicks > 240) fail();
        } else {
            stuckTicks = 0;
            lastPos = p;
        }
    }

    protected void lookAt(Villager v, BlockPos pos) {
        v.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
    }

    protected void lookAt(Villager v, Entity e) {
        v.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(e, true));
    }

    /** Shows an item between the villager's crossed arms, only if its hand is empty. */
    protected void carry(Villager v, ItemStack stack) {
        if (stack.isEmpty() || !v.getMainHandItem().isEmpty()) return;
        carried = stack.copyWithCount(1);
        v.setItemSlot(EquipmentSlot.MAINHAND, carried);
        v.setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    /** Removes the shown item, but only if it is still the one this task gave it. */
    protected void dropCarried(Villager v) {
        if (!carried.isEmpty() && v.getMainHandItem() == carried) {
            v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        carried = ItemStack.EMPTY;
    }

    protected void hop(Villager v) {
        if (v.onGround()) {
            v.setDeltaMovement(v.getDeltaMovement().add(0, 0.38, 0));
            v.hasImpulse = true;
        }
    }

    protected static void particles(ServerLevel level, Vec3 at, ParticleOptions type, int count, double spread) {
        level.sendParticles(type, at.x, at.y, at.z, count, spread, spread * 0.6, spread, 0.02);
    }

    protected static void sound(ServerLevel level, Villager v, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, v.getX(), v.getY(), v.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    protected static Vec3 head(Villager v) {
        return v.position().add(0, v.getBbHeight() + 0.3, 0);
    }
}
