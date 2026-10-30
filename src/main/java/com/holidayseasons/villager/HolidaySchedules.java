package com.holidayseasons.villager;

import com.holidayseasons.EverFest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Villager schedules used on celebration days. They are swapped in by {@link HolidayBrainInjector} only over vanilla's
 * default schedules and are never saved (brains rebuild their schedule on load), so a world without EverFest, or a
 * restart, always returns villagers to vanilla schedules.
 */
public final class HolidaySchedules {
    public static final DeferredRegister<Schedule> SCHEDULES = DeferredRegister.create(Registries.SCHEDULE, EverFest.MOD_ID);

    /** Adults: a short morning shift, then festivities at the square until dusk. */
    public static final DeferredHolder<Schedule, Schedule> FESTIVAL = SCHEDULES.register("villager_festival",
            () -> new ScheduleBuilder(new Schedule())
                    .changeActivityAt(10, Activity.IDLE)
                    .changeActivityAt(1500, Activity.WORK)
                    .changeActivityAt(4500, Activity.IDLE)
                    .changeActivityAt(7500, Activity.MEET)
                    .changeActivityAt(11500, Activity.IDLE)
                    .changeActivityAt(12500, Activity.REST)
                    .build());
    /** Adults: party at the square until after midnight (New Year's). Festival lights keep the square spawn-proof. */
    public static final DeferredHolder<Schedule, Schedule> NIGHT_PARTY = SCHEDULES.register("villager_night_party",
            () -> new ScheduleBuilder(new Schedule())
                    .changeActivityAt(10, Activity.IDLE)
                    .changeActivityAt(2000, Activity.WORK)
                    .changeActivityAt(6000, Activity.IDLE)
                    .changeActivityAt(9000, Activity.MEET)
                    .changeActivityAt(18600, Activity.REST)
                    .build());
    /** Children: more play time, home at dusk. */
    public static final DeferredHolder<Schedule, Schedule> BABY_FESTIVAL = SCHEDULES.register("villager_baby_festival",
            () -> new ScheduleBuilder(new Schedule())
                    .changeActivityAt(10, Activity.IDLE)
                    .changeActivityAt(1000, Activity.PLAY)
                    .changeActivityAt(6000, Activity.IDLE)
                    .changeActivityAt(7000, Activity.PLAY)
                    .changeActivityAt(12000, Activity.REST)
                    .build());
    /** Children on Halloween: out playing (trick-or-treating) into early dusk, home before full night. */
    public static final DeferredHolder<Schedule, Schedule> BABY_DUSK = SCHEDULES.register("villager_baby_dusk",
            () -> new ScheduleBuilder(new Schedule())
                    .changeActivityAt(10, Activity.IDLE)
                    .changeActivityAt(2000, Activity.PLAY)
                    .changeActivityAt(6000, Activity.IDLE)
                    .changeActivityAt(8000, Activity.PLAY)
                    .changeActivityAt(13300, Activity.REST)
                    .build());

    private HolidaySchedules() {
    }

    public static void register(IEventBus modBus) {
        SCHEDULES.register(modBus);
    }

    public static boolean isOurs(Schedule s) {
        return s == FESTIVAL.get() || s == NIGHT_PARTY.get() || s == BABY_FESTIVAL.get() || s == BABY_DUSK.get();
    }
}
