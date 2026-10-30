package com.holidayseasons.calendar;

import com.holidayseasons.EverFest;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.network.CalendarSyncPayload;
import com.holidayseasons.seasons.SeasonAdapter;
import com.holidayseasons.seasons.SeasonAdapters;
import com.holidayseasons.seasons.SeasonReading;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-side owner of "what day is it". Ticks once per server tick, keeps {@link CalendarClockData} in step with
 * the overworld clock, and publishes a {@link CalendarSnapshot} to gameplay systems and clients.
 */
public final class CalendarService {
    private final MinecraftServer server;
    private final CalendarClockData data;
    private CalendarSettings settings;
    @Nullable
    private CalendarSnapshot current;
    @Nullable
    private CalendarSnapshot seasonSchedule;
    private long ticksSinceSync;
    /**
     * Target of an in-flight Serene Seasons date change. Setting an SS date takes two steps (season set, then the clock
     * delta is applied on SS's next tick); date events are held until SS reaches the target so holidays never react
     * to the intermediate date.
     */
    @Nullable
    private CalendarDate pendingSeasonTarget;
    private int pendingTicksLeft;

    public CalendarService(MinecraftServer server) {
        this.server = server;
        this.data = server.overworld().getDataStorage().computeIfAbsent(CalendarClockData.FACTORY, CalendarClockData.FILE_ID);
        reloadSettings();
    }

    public void reloadSettings() {
        this.settings = ServerConfig.INSTANCE.calendarSettings();
        for (String problem : settings.problems()) {
            EverFest.LOGGER.warn("EverFest calendar config: {}", problem);
        }
        if (settings.mode() == CalendarMode.CUSTOM && ServerConfig.SPEC.isLoaded() && ServerConfig.INSTANCE.warnOnSeasonMismatch.get()) {
            seasonReading().ifPresent(r -> {
                int recommended = settings.recommendedSubSeasonDays();
                if (r.daysPerSubSeason() != recommended) {
                    EverFest.LOGGER.warn("EverFest: CUSTOM calendar months average {} days but Serene Seasons sub-seasons last {} days. "
                            + "Set Serene Seasons' sub_season_duration to {} to keep seasons aligned with months.",
                            recommended, r.daysPerSubSeason(), recommended);
                }
            });
        }
        // Force an immediate resync with the new settings.
        this.ticksSinceSync = Long.MAX_VALUE / 2;
    }

    public CalendarSettings settings() {
        return settings;
    }

    public CalendarClockData data() {
        return data;
    }

    public SeasonAdapter seasonAdapter() {
        return SeasonAdapters.get();
    }

    public Optional<SeasonReading> seasonReading() {
        return SeasonAdapters.get().read(server.overworld());
    }

    /** Called at the end of every server tick. */
    public void tick() {
        ServerLevel overworld = server.overworld();
        long dayTime = overworld.getDayTime();
        if (data.lastDayTime == Long.MIN_VALUE) {
            // Brand new calendar (or upgraded world): align calendar time-of-day with the world clock.
            data.calendarTicks = Math.floorMod(dayTime, ClockMath.TICKS_PER_DAY);
            data.lastDayTime = dayTime;
            data.setDirty();
        } else if (dayTime != data.lastDayTime) {
            data.calendarTicks = ClockMath.applyDayTimeChange(data.calendarTicks, data.lastDayTime, dayTime, settings.timeJumpPolicy());
            data.lastDayTime = dayTime;
            data.setDirty();
        }

        Optional<SeasonReading> reading = seasonReading();
        reading.ifPresent(this::trackSeasonYear);

        CalendarSnapshot next = computeSnapshot(reading.orElse(null), dayTime);
        if (pendingSeasonTarget != null) {
            if (next.date().equals(pendingSeasonTarget) || --pendingTicksLeft <= 0) {
                if (!next.date().equals(pendingSeasonTarget)) {
                    EverFest.LOGGER.warn("EverFest: Serene Seasons did not reach {} (now {})", pendingSeasonTarget, next.date());
                }
                pendingSeasonTarget = null;
            } else {
                return;
            }
        }
        CalendarSnapshot previous = current;
        current = next;
        seasonSchedule = reading.map(r -> seasonSnapshot(r, dayTime)).orElse(null);

        boolean dateChanged = previous == null
                || !previous.date().equals(next.date())
                || previous.mode() != next.mode()
                || !previous.systemSignature().equals(next.systemSignature())
                || previous.subSeason() != next.subSeason();
        if (dateChanged) {
            if (previous != null) {
                EverFest.LOGGER.debug("EverFest calendar: {} -> {}", previous.date(), next.date());
            }
            NeoForge.EVENT_BUS.post(new CalendarDateChangedEvent(server, previous, next));
        }
        ticksSinceSync++;
        int interval = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.clientSyncIntervalTicks.get() : 200;
        if (dateChanged || ticksSinceSync >= interval) {
            ticksSinceSync = 0;
            PacketDistributor.sendToAllPlayers(new CalendarSyncPayload(next));
        }
    }

    private void trackSeasonYear(SeasonReading r) {
        long yearTicks = SeasonCycleMapper.yearTicks(r.cycleTicks(), r.subSeasonTicks());
        if (data.lastSeasonYearTicks < 0) {
            data.lastSeasonYearTicks = yearTicks;
            data.setDirty();
            return;
        }
        if (yearTicks != data.lastSeasonYearTicks) {
            int newYear = ClockMath.trackCycleYear(data.seasonYear, data.lastSeasonYearTicks, yearTicks, r.cycleLength());
            if (newYear != data.seasonYear) {
                EverFest.LOGGER.info("EverFest: season calendar year {} -> {}", data.seasonYear, newYear);
            }
            data.seasonYear = newYear;
            data.lastSeasonYearTicks = yearTicks;
            data.setDirty();
        }
    }

    public void sendTo(ServerPlayer player) {
        if (current != null) PacketDistributor.sendToPlayer(player, new CalendarSyncPayload(current));
    }

    public CalendarSnapshot snapshot() {
        if (current == null) current = computeSnapshot(seasonReading().orElse(null), server.overworld().getDayTime());
        return current;
    }

    /**
     * The snapshot the holiday engine schedules on. Identical to {@link #snapshot()} unless the FOLLOW_SEASON policy
     * is active and Serene Seasons is installed, in which case holidays run on the seasonal cycle.
     */
    public CalendarSnapshot schedulingSnapshot() {
        CalendarSnapshot now = snapshot();
        if (settings.seasonSyncPolicy() == SeasonSyncPolicy.FOLLOW_SEASON && seasonSchedule != null) return seasonSchedule;
        return now;
    }

    /** The calendar system matching {@link #schedulingSnapshot()}. */
    public CalendarSystem schedulingSystem() {
        CalendarSnapshot s = schedulingSnapshot();
        return systemFor(s);
    }

    public CalendarSystem systemFor(CalendarSnapshot s) {
        if (s.mode() == CalendarMode.REAL_WORLD && s.systemSignature().equals(GregorianCalendarSystem.INSTANCE.signature())) {
            return GregorianCalendarSystem.INSTANCE;
        }
        if (s.systemSignature().equals(settings.customSystem().signature())) return settings.customSystem();
        // Uniform systems are fully described by their month length.
        return settings.seasonSystem(s.monthLengths()[0]);
    }

    public CalendarSystem activeSystem() {
        return systemFor(snapshot());
    }

    private CalendarSnapshot computeSnapshot(@Nullable SeasonReading reading, long dayTime) {
        return switch (settings.mode()) {
            case SEASON_SYNCED -> reading != null ? seasonSnapshot(reading, dayTime) : fallbackSnapshot(dayTime);
            case CUSTOM -> customSnapshot(reading);
            case REAL_WORLD -> realWorldSnapshot(reading);
        };
    }

    private CalendarSnapshot seasonSnapshot(SeasonReading r, long dayTime) {
        FixedMonthCalendarSystem system = settings.seasonSystem(r.daysPerSubSeason());
        CalendarDate date = SeasonCycleMapper.dateFor(data.seasonYear, r.cycleTicks(), r.subSeasonTicks(), r.dayDuration(), system);
        long epochDay = system.toEpochDay(date);
        String iso = date.toIsoString();
        if (settings.mode() == CalendarMode.SEASON_SYNCED && !iso.equals(data.lastSyncedDate)) {
            data.lastSyncedDate = iso;
            data.setDirty();
        }
        return new CalendarSnapshot(CalendarMode.SEASON_SYNCED, system.signature(), date, epochDay,
                (int) Math.floorMod(dayTime, ClockMath.TICKS_PER_DAY), system.dayOfWeek(epochDay),
                r.subSeason().season(), r.subSeason(), true, CalendarSnapshot.monthLengthsOf(system, date.year()));
    }

    private CalendarSnapshot fallbackSnapshot(long dayTime) {
        FixedMonthCalendarSystem system = settings.seasonSystem(settings.fallbackDaysPerMonth());
        long start = system.toEpochDay(new CalendarDate(1, SubSeason.CYCLE_START_MONTH, 1));
        if (data.lastSyncedDate != null) {
            // Serene Seasons was removed (or failed): continue from the last date it produced instead of jumping.
            try {
                CalendarDate last = CalendarDate.parseIso(data.lastSyncedDate);
                CalendarDate carried = system.clamp(last.year(), last.month(), last.day());
                data.fallbackDayOffset = system.toEpochDay(carried) - start - ClockMath.dayIndex(data.calendarTicks);
                EverFest.LOGGER.info("EverFest: Serene Seasons unavailable; calendar continues from {}", carried);
            } catch (RuntimeException e) {
                EverFest.LOGGER.warn("EverFest: could not carry over last synced date '{}'", data.lastSyncedDate);
            }
            data.lastSyncedDate = null;
            data.setDirty();
        }
        long epochDay = Math.max(0, start + ClockMath.dayIndex(data.calendarTicks) + data.fallbackDayOffset);
        CalendarDate date = system.fromEpochDay(epochDay);
        SubSeason sub = SubSeason.forMonth(date.month());
        return new CalendarSnapshot(CalendarMode.SEASON_SYNCED, system.signature(), date, epochDay,
                ClockMath.timeOfDay(data.calendarTicks), system.dayOfWeek(epochDay), sub.season(), sub, false,
                CalendarSnapshot.monthLengthsOf(system, date.year()));
    }

    private CalendarSnapshot customSnapshot(@Nullable SeasonReading reading) {
        FixedMonthCalendarSystem system = settings.customSystem();
        long start = system.toEpochDay(settings.customStartDate());
        long epochDay = Math.max(0, start + ClockMath.dayIndex(data.calendarTicks) + data.customDayOffset);
        CalendarDate date = system.fromEpochDay(epochDay);
        SubSeason sub = reading != null ? reading.subSeason() : SubSeason.forMonth(date.month());
        return new CalendarSnapshot(CalendarMode.CUSTOM, system.signature(), date, epochDay,
                ClockMath.timeOfDay(data.calendarTicks), system.dayOfWeek(epochDay), sub.season(), sub, reading != null,
                CalendarSnapshot.monthLengthsOf(system, date.year()));
    }

    private CalendarSnapshot realWorldSnapshot(@Nullable SeasonReading reading) {
        ZonedDateTime now = ZonedDateTime.now(settings.realWorldZone());
        LocalDate local;
        if (data.realDateOverride != null) {
            try {
                local = LocalDate.parse(data.realDateOverride);
            } catch (RuntimeException e) {
                EverFest.LOGGER.warn("EverFest: ignoring invalid real-world date override '{}'", data.realDateOverride);
                data.realDateOverride = null;
                data.setDirty();
                local = now.toLocalDate();
            }
        } else {
            local = now.toLocalDate().plusDays(data.realDayOffset);
        }
        GregorianCalendarSystem system = GregorianCalendarSystem.INSTANCE;
        CalendarDate date = GregorianCalendarSystem.fromLocalDate(local);
        SubSeason sub = reading != null ? reading.subSeason() : SubSeason.forMonth(date.month());
        int tod = ClockMath.timeOfDayFromWallClock(now.getHour(), now.getMinute(), now.getSecond());
        return new CalendarSnapshot(CalendarMode.REAL_WORLD, system.signature(), date, local.toEpochDay(), tod,
                local.getDayOfWeek().getValue(), sub.season(), sub, reading != null,
                CalendarSnapshot.monthLengthsOf(system, date.year()));
    }

    // ------------------------------------------------------------------ admin controls

    /** Shifts the date by {@code days}. Returns a human readable description of what was done. */
    public String addDays(long days) {
        switch (settings.mode()) {
            case CUSTOM -> data.customDayOffset += days;
            case REAL_WORLD -> {
                if (data.realDateOverride != null) {
                    data.realDateOverride = LocalDate.parse(data.realDateOverride).plusDays(days).toString();
                } else {
                    data.realDayOffset += days;
                }
            }
            case SEASON_SYNCED -> {
                if (seasonReading().isPresent()) {
                    // Serene Seasons follows the world clock; advancing the clock is its supported interface.
                    if (days < 0) return "Serene Seasons cannot run backwards; use /everfest date set";
                    runCommand("time add " + (days * ClockMath.TICKS_PER_DAY));
                    return "Advanced the world clock by " + days + " days";
                }
                data.fallbackDayOffset += days;
            }
        }
        data.setDirty();
        forceRefresh();
        return "Shifted the calendar by " + days + " days";
    }

    /** Sets the current date. Returns an error message, or null on success. */
    @Nullable
    public String setDate(CalendarDate target) {
        switch (settings.mode()) {
            case CUSTOM -> {
                FixedMonthCalendarSystem system = settings.customSystem();
                if (target.year() < 1 || !system.isValid(target)) return target + " does not exist in this calendar";
                long start = system.toEpochDay(settings.customStartDate());
                long elapsed = ClockMath.dayIndex(data.calendarTicks);
                data.customDayOffset = system.toEpochDay(target) - start - elapsed;
            }
            case REAL_WORLD -> {
                try {
                    data.realDateOverride = LocalDate.of(target.year(), target.month(), target.day()).toString();
                } catch (RuntimeException e) {
                    return target + " is not a valid date";
                }
            }
            case SEASON_SYNCED -> {
                Optional<SeasonReading> reading = seasonReading();
                if (reading.isEmpty()) {
                    FixedMonthCalendarSystem system = settings.seasonSystem(settings.fallbackDaysPerMonth());
                    if (target.year() < 1 || !system.isValid(target)) return target + " does not exist in this calendar";
                    long start = system.toEpochDay(new CalendarDate(1, SubSeason.CYCLE_START_MONTH, 1));
                    data.fallbackDayOffset = system.toEpochDay(target) - start - ClockMath.dayIndex(data.calendarTicks);
                } else {
                    SeasonReading r = reading.get();
                    int days = r.daysPerSubSeason();
                    if (target.year() < 1 || target.day() > days) return target + " does not exist (months have " + days + " days)";
                    SubSeason sub = SubSeason.forMonth(target.month());
                    // Use Serene Seasons' own public command to move its cycle, then advance the clock to the day.
                    runCommand("season set " + sub.id());
                    if (target.day() > 1) runCommand("time add " + ((target.day() - 1L) * r.dayDuration()));
                    long subTicks = r.subSeasonTicks();
                    data.seasonYear = target.year();
                    data.lastSeasonYearTicks = SeasonCycleMapper.yearTicks(sub.ordinal() * subTicks + (target.day() - 1L) * r.dayDuration(), subTicks);
                    pendingSeasonTarget = target;
                    pendingTicksLeft = 100;
                }
            }
        }
        data.setDirty();
        forceRefresh();
        return null;
    }

    public void clearRealWorldOverride() {
        data.realDateOverride = null;
        data.realDayOffset = 0;
        data.setDirty();
        forceRefresh();
    }

    private void runCommand(String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
    }

    private void forceRefresh() {
        ticksSinceSync = Long.MAX_VALUE / 2;
    }

    public MinecraftServer server() {
        return server;
    }

    public boolean sameDate(CalendarSnapshot a, CalendarSnapshot b) {
        return Objects.equals(a.date(), b.date()) && a.systemSignature().equals(b.systemSignature());
    }
}
