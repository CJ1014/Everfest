package com.holidayseasons.villager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.holidayseasons.ai.CelebrationBehavior;
import com.holidayseasons.ai.CelebrationDirector;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Adds EverFest's {@link CelebrationBehavior} to a villager's brain <em>alongside</em> vanilla behaviours.
 *
 * <p>{@code Brain.addActivity*} appends behaviours to an activity and replaces only that activity's entry requirements,
 * so we re-supply exactly vanilla's requirements: none for IDLE and PLAY, a meeting point for MEET. Vanilla's own
 * behaviours are untouched. Brains are rebuilt by {@code Villager.refreshBrain} (profession change, growing up) and on
 * load, so injection is tracked per brain instance and repeated when needed.</p>
 */
public final class HolidayBrainInjector {
    private static final Map<Brain<?>, Boolean> INJECTED = Collections.synchronizedMap(new WeakHashMap<>());

    private HolidayBrainInjector() {
    }

    public static boolean isInjected(Villager v) {
        return INJECTED.containsKey(v.getBrain());
    }

    public static void ensure(Villager v, CelebrationDirector director) {
        Brain<Villager> brain = v.getBrain();
        if (INJECTED.containsKey(brain)) return;
        // Separate instances per activity: a shared instance would be ticked once per activity it is listed under.
        brain.addActivity(Activity.IDLE, ImmutableList.of(Pair.of(2, new CelebrationBehavior(director))));
        brain.addActivity(Activity.PLAY, ImmutableList.of(Pair.of(2, new CelebrationBehavior(director))));
        brain.addActivityWithConditions(Activity.MEET, ImmutableList.of(Pair.of(2, new CelebrationBehavior(director))),
                ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        INJECTED.put(brain, Boolean.TRUE);
    }

    /** Applies or removes the festival schedule. Only ever replaces vanilla's default schedules. */
    public static void applySchedule(Villager v, boolean festival, boolean nightParty, boolean duskPlay) {
        Brain<Villager> brain = v.getBrain();
        Schedule current = brain.getSchedule();
        boolean vanilla = current == Schedule.VILLAGER_DEFAULT || current == Schedule.VILLAGER_BABY;
        boolean ours = HolidaySchedules.isOurs(current);
        if (festival) {
            if (!vanilla && !ours) return;
            Schedule wanted = v.isBaby()
                    ? (duskPlay ? HolidaySchedules.BABY_DUSK.get() : HolidaySchedules.BABY_FESTIVAL.get())
                    : (nightParty ? HolidaySchedules.NIGHT_PARTY.get() : HolidaySchedules.FESTIVAL.get());
            if (current != wanted) brain.setSchedule(wanted);
        } else if (ours) {
            brain.setSchedule(v.isBaby() ? Schedule.VILLAGER_BABY : Schedule.VILLAGER_DEFAULT);
        }
    }

    /** Count of behaviours registered for an activity - used by GameTests to prove vanilla behaviours are kept. */
    public static int behaviourCount(Villager v, Activity activity) {
        int[] n = {0};
        try {
            java.lang.reflect.Field f = Brain.class.getDeclaredField("availableBehaviorsByPriority");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Integer, Map<Activity, java.util.Set<?>>> map = (Map<Integer, Map<Activity, java.util.Set<?>>>) f.get(v.getBrain());
            map.values().forEach(m -> {
                java.util.Set<?> set = m.get(activity);
                if (set != null) n[0] += set.size();
            });
        } catch (ReflectiveOperationException e) {
            return -1;
        }
        return n[0];
    }
}
