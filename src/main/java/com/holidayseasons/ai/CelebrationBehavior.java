package com.holidayseasons.ai;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Runs the holiday task the {@link CelebrationDirector} assigned to a villager. Lives in the villager's brain next to
 * vanilla behaviours and yields immediately to panic, sleep, trading, raids and schedule changes to work or rest.
 */
public final class CelebrationBehavior extends Behavior<Villager> {
    private final CelebrationDirector director;
    /** The copy living in the WORK activity only runs party invitations: a host's invitation calls a villager from work. */
    private final boolean invitationsOnly;
    private CelebrationTask running;

    public CelebrationBehavior(CelebrationDirector director) {
        this(director, false);
    }

    public CelebrationBehavior(CelebrationDirector director, boolean invitationsOnly) {
        // Tasks end themselves; the behaviour's own time-out is only a safety net.
        super(ImmutableMap.of(), 6000, 6000);
        this.director = director;
        this.invitationsOnly = invitationsOnly;
    }

    private boolean eligibleFor(Villager v, CelebrationTask task) {
        if (!invitationsOnly) return eligible(v);
        if (task.type != TaskType.PARTY_GUEST || v.isSleeping() || v.isTrading() || !v.isAlive()) return false;
        Brain<Villager> brain = v.getBrain();
        return brain.isActive(Activity.WORK) && !brain.isActive(Activity.PANIC) && !brain.isActive(Activity.RAID)
                && !brain.isActive(Activity.PRE_RAID) && !brain.isActive(Activity.HIDE);
    }

    static boolean eligible(Villager v) {
        Brain<Villager> brain = v.getBrain();
        if (v.isSleeping() || v.isTrading() || !v.isAlive()) return false;
        if (brain.isActive(Activity.PANIC) || brain.isActive(Activity.REST) || brain.isActive(Activity.RAID)
                || brain.isActive(Activity.PRE_RAID) || brain.isActive(Activity.HIDE) || brain.isActive(Activity.WORK)) {
            return false;
        }
        return brain.isActive(Activity.IDLE) || brain.isActive(Activity.MEET) || brain.isActive(Activity.PLAY);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager owner) {
        CelebrationTask task = director.taskFor(owner);
        return task != null && !task.isDone() && !task.isRunning() && eligibleFor(owner, task);
    }

    @Override
    protected void start(ServerLevel level, Villager v, long gameTime) {
        running = director.taskFor(v);
        if (running != null) running.begin(level, v);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager v, long gameTime) {
        return running != null && running == director.taskFor(v) && !running.isDone() && eligibleFor(v, running);
    }

    @Override
    protected void tick(ServerLevel level, Villager v, long gameTime) {
        if (running != null) running.tickTask(level, v);
    }

    @Override
    protected void stop(ServerLevel level, Villager v, long gameTime) {
        if (running != null) {
            running.end(level, v);
            director.onTaskStopped(v, running);
        }
        running = null;
    }
}
