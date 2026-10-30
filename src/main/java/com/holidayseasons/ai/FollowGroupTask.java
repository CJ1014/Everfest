package com.holidayseasons.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;

/** A trick-or-treater following its group's leader and sharing its reactions. */
public final class FollowGroupTask extends CelebrationTask {
    private final TrickOrTreatGroup group;
    private TrickOrTreatGroup.Phase seen = TrickOrTreatGroup.Phase.WALKING;
    private int reactTicks;

    public FollowGroupTask(String occ, String village, TrickOrTreatGroup group) {
        super(TaskType.TRICK_OR_TREAT, occ, village);
        this.group = group;
    }

    @Override
    protected int maxTicks() {
        return 9000;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (!group.isActive()) {
            finish();
            return;
        }
        Villager leader = group.leader;
        if (group.phase != seen) {
            seen = group.phase;
            reactTicks = 0;
            if (seen == TrickOrTreatGroup.Phase.TREAT) {
                carry(v, group.lastCandy);
                particles(level, head(v), ParticleTypes.HAPPY_VILLAGER, 4, 0.3);
                sound(level, v, SoundEvents.VILLAGER_CELEBRATE, 0.6f, 1.6f + level.random.nextFloat() * 0.2f);
            } else if (seen == TrickOrTreatGroup.Phase.TRICK) {
                sound(level, v, SoundEvents.VILLAGER_AMBIENT, 0.6f, 1.8f);
            } else if (seen == TrickOrTreatGroup.Phase.WALKING) {
                dropCarried(v);
            }
        }
        reactTicks++;
        if ((seen == TrickOrTreatGroup.Phase.TREAT || seen == TrickOrTreatGroup.Phase.TRICK) && reactTicks % 14 == 0) hop(v);
        if (seen == TrickOrTreatGroup.Phase.KNOCKING || seen == TrickOrTreatGroup.Phase.WAITING) {
            if (group.current != null) lookAt(v, group.current.door());
            if (v.distanceToSqr(leader) > 9) walkTo(v, leader, 0.55f, 2);
            return;
        }
        if (v.distanceToSqr(leader) > 6) {
            walkTo(v, leader, 0.6f, 2);
            if (failed()) {
                clearFailure(v);
                if (v.distanceToSqr(leader) > 32 * 32) finish();
            }
        } else {
            lookAt(v, leader);
        }
    }
}
