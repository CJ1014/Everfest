package com.holidayseasons.holiday;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarService;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.network.HolidayNoticePayload;
import com.holidayseasons.network.HolidayStatusPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side holiday scheduler. Reconciles the persisted lifecycle with the scheduling calendar whenever the date,
 * config or datapacks change, posts {@link HolidayPhaseChangedEvent}s, announces holidays and syncs statuses.
 */
public final class HolidayService {
    private final MinecraftServer server;
    private final CalendarService calendar;
    private final HolidayStateData data;
    private List<ScheduledHoliday> holidays = List.of();
    private Map<String, ScheduledHoliday> byId = Map.of();
    private boolean dirty = true;
    /** Recent transitions (newest last), for diagnostics and tests. Not persisted. */
    private final java.util.ArrayDeque<HolidayTransition> recent = new java.util.ArrayDeque<>();

    public HolidayService(MinecraftServer server, CalendarService calendar) {
        this.server = server;
        this.calendar = calendar;
        this.data = server.overworld().getDataStorage().computeIfAbsent(HolidayStateData.FACTORY, HolidayStateData.FILE_ID);
        reloadDefinitions();
    }

    /** Re-reads datapack definitions and config overrides; reconciles on the next tick. */
    public void reloadDefinitions() {
        holidays = HolidayRegistry.scheduled();
        Map<String, ScheduledHoliday> map = new HashMap<>();
        for (ScheduledHoliday h : holidays) map.put(h.id(), h);
        byId = map;
        dirty = true;
    }

    public void markDirty() {
        dirty = true;
    }

    public void tick() {
        if (dirty) {
            dirty = false;
            reconcile();
        }
    }

    public HolidayEngineCore.Context context() {
        CalendarSnapshot s = calendar.schedulingSnapshot();
        CalendarSystem system = calendar.systemFor(s);
        return new HolidayEngineCore.Context(system, s.epochDay(), s.date().year(), s.season(), calendar.settings().seasonSyncPolicy());
    }

    public void reconcile() {
        HolidayEngineCore.Context ctx = context();
        List<HolidayTransition> transitions = HolidayEngineCore.reconcile(data.records, holidays, ctx);
        int historyYears = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.historyYears.get() : 3;
        int pruned = HolidayEngineCore.prune(data.records, ctx.currentYear(), historyYears);
        if (!transitions.isEmpty() || pruned > 0) data.setDirty();
        for (HolidayTransition t : transitions) {
            recent.addLast(t);
            while (recent.size() > 200) recent.removeFirst();
            EverFest.LOGGER.info("EverFest holiday {} {} {} -> {}{}{}", t.holidayId(), t.year(), t.from(), t.to(),
                    t.kind() != HolidayTransition.Kind.ENTER ? " (" + t.kind() + ": " + t.reason() + ")" : "",
                    t.transientStep() ? " [skipped through]" : "");
            ScheduledHoliday h = byId.get(t.holidayId());
            if (h != null) {
                NeoForge.EVENT_BUS.post(new HolidayPhaseChangedEvent(server, h, t));
                announce(h, t);
            }
        }
        broadcastStatuses();
    }

    private void announce(ScheduledHoliday h, HolidayTransition t) {
        if (t.kind() != HolidayTransition.Kind.ENTER || t.transientStep()) return;
        if (t.to() != HolidayPhase.PREPARING && t.to() != HolidayPhase.ACTIVE) return;
        if (ServerConfig.SPEC.isLoaded() && !ServerConfig.INSTANCE.announceHolidays.get()) return;
        Component name = Component.translatable(statusKey(h.id())).withColor(h.color() & 0xFFFFFF);
        Component msg = t.to() == HolidayPhase.PREPARING
                ? Component.translatable("holidayseasons.announce.preparing", name, Math.max(1, h.preparationDays()))
                : Component.translatable("holidayseasons.announce.active", name);
        server.getPlayerList().broadcastSystemMessage(Component.literal("❀ ").withStyle(ChatFormatting.GOLD).append(msg), false);
        PacketDistributor.sendToAllPlayers(new HolidayNoticePayload(h.id(), t.to(), h.color()));
    }

    public static String statusKey(String holidayId) {
        int colon = holidayId.indexOf(':');
        return "holiday." + holidayId.substring(0, colon) + "." + holidayId.substring(colon + 1);
    }

    // ------------------------------------------------------------------ queries

    public List<HolidayTransition> recentTransitions() {
        return List.copyOf(recent);
    }

    public List<ScheduledHoliday> holidays() {
        return holidays;
    }

    public Optional<ScheduledHoliday> holiday(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Holidays currently in the given phase. */
    public List<ScheduledHoliday> inPhase(HolidayPhase phase) {
        List<ScheduledHoliday> out = new ArrayList<>();
        for (OccurrenceRecord r : data.records.values()) {
            if (r.phase == phase) {
                ScheduledHoliday h = byId.get(r.holidayId);
                if (h != null && h.enabled()) out.add(h);
            }
        }
        return out;
    }

    /** The live (preparing/active/cleanup) occurrence of a holiday, if any. */
    public Optional<OccurrenceRecord> liveOccurrence(String holidayId) {
        return data.records.values().stream().filter(r -> r.holidayId.equals(holidayId) && r.phase.isLive()).findFirst();
    }

    public Map<String, OccurrenceRecord> records() {
        return data.records;
    }

    /** Statuses for each enabled holiday's occurrences last year, this year and next year. */
    public List<HolidayStatus> statuses() {
        HolidayEngineCore.Context ctx = context();
        CalendarSystem sys = ctx.system();
        List<HolidayStatus> out = new ArrayList<>();
        for (ScheduledHoliday h : holidays) {
            if (!h.enabled()) continue;
            for (int year = ctx.currentYear() - 1; year <= ctx.currentYear() + 1; year++) {
                if (year < 1) continue;
                HolidayWindow w = h.window(year, sys);
                OccurrenceRecord rec = data.records.get(OccurrenceRecord.key(h.id(), sys.signature(), year));
                HolidayPhase phase = HolidayEngineCore.displayPhase(data.records, h, year, ctx);
                out.add(new HolidayStatus(h.id(), h.color(), sys.fromEpochDay(w.prepStart()), sys.fromEpochDay(w.start()),
                        sys.fromEpochDay(w.end() - 1), phase, w.start() - ctx.today(), rec == null ? "" : rec.skipReason));
            }
        }
        out.sort(Comparator.comparing(HolidayStatus::start));
        return out;
    }

    /** Next occurrence that has not finished, for countdowns. */
    @Nullable
    public HolidayStatus nextStatus(String holidayId) {
        for (HolidayStatus s : statuses()) {
            if (s.id().equals(holidayId) && s.phase() != HolidayPhase.COMPLETED && s.skippedReason().isEmpty()) return s;
        }
        return null;
    }

    public void broadcastStatuses() {
        PacketDistributor.sendToAllPlayers(new HolidayStatusPayload(context().system().signature(), statuses()));
    }

    public void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new HolidayStatusPayload(context().system().signature(), statuses()));
    }

    // ------------------------------------------------------------------ admin

    /** Forgets the current occurrence of a holiday so it can be celebrated again (testing / events). */
    public boolean reset(String holidayId) {
        HolidayEngineCore.Context ctx = context();
        boolean changed = false;
        for (OccurrenceRecord r : new ArrayList<>(data.records.values())) {
            if (!r.holidayId.equals(holidayId) || Math.abs(r.year - ctx.currentYear()) > 1) continue;
            if (r.phase.isLive()) {
                HolidayTransition abort = new HolidayTransition(r.holidayId, r.key(), r.year, r.phase, HolidayPhase.INACTIVE,
                        HolidayTransition.Kind.ABORT, false, "admin_reset");
                ScheduledHoliday h = byId.get(holidayId);
                if (h != null) NeoForge.EVENT_BUS.post(new HolidayPhaseChangedEvent(server, h, abort));
            }
            data.records.remove(r.key());
            changed = true;
        }
        if (changed) {
            data.setDirty();
            dirty = true;
        }
        return changed;
    }

    /** Ends the live occurrence of a holiday immediately (cleanup and complete). */
    public boolean finish(String holidayId) {
        Optional<OccurrenceRecord> live = liveOccurrence(holidayId);
        if (live.isEmpty()) return false;
        OccurrenceRecord r = live.get();
        ScheduledHoliday h = byId.get(holidayId);
        HolidayTransition t = new HolidayTransition(r.holidayId, r.key(), r.year, r.phase, HolidayPhase.COMPLETED,
                HolidayTransition.Kind.ABORT, false, "admin_finish");
        r.phase = HolidayPhase.COMPLETED;
        r.skipReason = "admin_finish";
        data.setDirty();
        if (h != null) NeoForge.EVENT_BUS.post(new HolidayPhaseChangedEvent(server, h, t));
        broadcastStatuses();
        return true;
    }
}
