package com.holidayseasons.ai;

import com.holidayseasons.event.HalloweenService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Leads a trick-or-treat group from door to door: choose a reachable door that has not been visited, walk there,
 * knock (or ring the doorbell), wait for an answer, receive candy or play a harmless trick, react, remember the visit
 * and move on. Ends after a few doors, when children have to go home, or when Halloween ends.
 */
public final class TrickOrTreatTask extends CelebrationTask {
    private static final int WAIT_TICKS = 60;
    private static final int OWNER_WAIT_TICKS = 220;
    private static final int REACT_TICKS = 50;

    private final HalloweenService service;
    private final TrickOrTreatGroup group;
    private final int maxDoors;
    private int phaseTicks;
    private int failedDestinations;
    private boolean waitingForOwner;
    private int searchAttempts;
    private int searchCooldown;

    public TrickOrTreatTask(String occ, String village, HalloweenService service, TrickOrTreatGroup group, int maxDoors) {
        super(TaskType.TRICK_OR_TREAT, occ, village);
        this.service = service;
        this.group = group;
        this.maxDoors = maxDoors;
    }

    public TrickOrTreatGroup group() {
        return group;
    }

    @Override
    protected int maxTicks() {
        return 9000;
    }

    @Override
    protected void onStart(ServerLevel level, Villager v) {
        pickNext(level, v);
    }

    @Override
    protected void onEnd(ServerLevel level, Villager v) {
        group.phase = TrickOrTreatGroup.Phase.DONE;
        service.groupFinished(group);
    }

    private void pickNext(ServerLevel level, Villager v) {
        if (group.doorsVisited >= maxDoors) {
            finish();
            return;
        }
        TrickOrTreatGroup.Destination d = service.pickDestination(level, v, villageKey, occurrenceKey, group);
        if (d == null) {
            // Nothing found (or the child was still landing): look again shortly, a few times, then go home.
            group.current = null;
            if (++searchAttempts > 5) finish();
            else searchCooldown = 20;
            return;
        }
        searchAttempts = 0;
        group.current = d;
        group.phase = TrickOrTreatGroup.Phase.WALKING;
        phaseTicks = 0;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        TrickOrTreatGroup.Destination d = group.current;
        if (d == null) {
            if (--searchCooldown <= 0) pickNext(level, v);
            return;
        }
        phaseTicks++;
        switch (group.phase) {
            case WALKING -> {
                if (walkToDoor(v, d.entrance())) {
                    group.phase = TrickOrTreatGroup.Phase.KNOCKING;
                    phaseTicks = 0;
                } else if (failed()) {
                    // Could not reach this door: remember it and try another (bounded).
                    group.visited.add(d.key());
                    if (++failedDestinations > 3) return;
                    clearFailure(v);
                    pickNext(level, v);
                }
            }
            case KNOCKING -> {
                lookAt(v, d.door());
                if (phaseTicks == 10) service.knock(level, d, v);
                if (phaseTicks >= 25) {
                    group.phase = TrickOrTreatGroup.Phase.WAITING;
                    phaseTicks = 0;
                    waitingForOwner = service.notifyOwner(level, d, group);
                }
            }
            case WAITING -> {
                lookAt(v, d.door());
                ItemStack candy = service.tryAnswer(level, d, villageKey, group, phaseTicks);
                if (!candy.isEmpty()) {
                    treat(level, v, candy);
                } else if (phaseTicks >= (waitingForOwner ? OWNER_WAIT_TICKS : WAIT_TICKS)) {
                    ItemStack late = service.finalAnswer(level, d, villageKey, occurrenceKey, group);
                    if (!late.isEmpty()) treat(level, v, late);
                    else trick(level, v, d);
                }
            }
            case TREAT, TRICK -> {
                if (phaseTicks % 12 == 0) hop(v);
                if (phaseTicks >= REACT_TICKS) {
                    dropCarried(v);
                    group.visited.add(d.key());
                    group.doorsVisited++;
                    service.recordVisit(villageKey, occurrenceKey, d);
                    pickNext(level, v);
                }
            }
            default -> finish();
        }
    }

    /** Walks to the door; failure (unreachable) is signalled through {@link #failed()}. */
    private boolean walkToDoor(Villager v, BlockPos entrance) {
        return walkTo(v, entrance, 0.55f, 1);
    }

    private void treat(ServerLevel level, Villager v, ItemStack candy) {
        group.phase = TrickOrTreatGroup.Phase.TREAT;
        group.lastCandy = candy.copyWithCount(1);
        group.treats++;
        phaseTicks = 0;
        carry(v, group.lastCandy);
        Vec3 at = head(v);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 6, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 0.3, at.z, 2, 0.3, 0.2, 0.3, 0.5);
        sound(level, v, SoundEvents.VILLAGER_CELEBRATE, 0.8f, 1.5f);
    }

    private void trick(ServerLevel level, Villager v, TrickOrTreatGroup.Destination d) {
        group.phase = TrickOrTreatGroup.Phase.TRICK;
        group.tricks++;
        phaseTicks = 0;
        service.playTrick(level, d, villageKey, occurrenceKey, v);
    }

    @Nullable
    public TrickOrTreatGroup.Destination destination() {
        return group.current;
    }
}
