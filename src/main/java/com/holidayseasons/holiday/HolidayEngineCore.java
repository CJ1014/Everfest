package com.holidayseasons.holiday;

import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.calendar.SeasonSyncPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, side-effect-free holiday lifecycle reconciliation. Given the persisted occurrence records and
 * "today", it advances every occurrence to the phase today implies and returns the transitions taken. Calling it
 * again with the same inputs returns no transitions (idempotent).
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Phases only move forward: INACTIVE -> PREPARING -> ACTIVE -> CLEANUP -> COMPLETED. Every intermediate phase
 *   is emitted, flagged {@code transientStep} when the date jumped over it.</li>
 *   <li>COMPLETED is terminal for an occurrence, so rewinding time never celebrates the same occurrence twice.</li>
 *   <li>If the date moves back before a live occurrence's preparation, it is ABORTed back to INACTIVE (its world
 *   changes are cleaned up) and may prepare again later; it still can only become ACTIVE once.</li>
 *   <li>Live occurrences of disabled/removed holidays or of a different calendar system are ABORTed to COMPLETED.</li>
 * </ul>
 */
public final class HolidayEngineCore {
    private HolidayEngineCore() {
    }

    public record Context(CalendarSystem system, long today, int currentYear, Season season, SeasonSyncPolicy policy) {
    }

    public static List<HolidayTransition> reconcile(Map<String, OccurrenceRecord> records, List<ScheduledHoliday> holidays, Context ctx) {
        List<HolidayTransition> out = new ArrayList<>();
        Map<String, ScheduledHoliday> byId = new HashMap<>();
        for (ScheduledHoliday h : holidays) byId.put(h.id(), h);
        String sig = ctx.system().signature();

        // 1. Existing live occurrences: finish, abort or advance them.
        for (OccurrenceRecord rec : records.values()) {
            if (!rec.phase.isLive()) continue;
            ScheduledHoliday h = byId.get(rec.holidayId);
            String reason = h == null ? "removed" : !h.enabled() ? "disabled" : !rec.signature.equals(sig) ? "calendar_changed" : null;
            if (reason != null) {
                out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, rec.phase, HolidayPhase.COMPLETED,
                        HolidayTransition.Kind.ABORT, false, reason));
                rec.phase = HolidayPhase.COMPLETED;
                rec.skipReason = reason;
                continue;
            }
            rec.setWindow(h.window(rec.year, ctx.system()));
            advance(rec, h, rec.window().phaseAt(ctx.today()), ctx, out);
        }

        // 2. Make sure each enabled holiday's relevant occurrence exists and is up to date.
        for (ScheduledHoliday h : holidays) {
            if (!h.enabled()) continue;
            for (int year = ctx.currentYear() - 1; year <= ctx.currentYear() + 1; year++) {
                if (year < 1 && !ctx.system().isGregorian()) continue;
                HolidayWindow w = h.window(year, ctx.system());
                HolidayPhase target = w.phaseAt(ctx.today());
                String key = OccurrenceRecord.key(h.id(), sig, year);
                OccurrenceRecord rec = records.get(key);
                if (rec == null) {
                    // Only track occurrences that are live now; past ones were never seen (e.g. world created later).
                    if (!w.isLiveOn(ctx.today())) continue;
                    rec = new OccurrenceRecord(h.id(), sig, year, w);
                    records.put(key, rec);
                } else {
                    rec.setWindow(w);
                }
                advance(rec, h, target, ctx, out);
            }
        }
        return out;
    }

    private static void advance(OccurrenceRecord rec, ScheduledHoliday h, HolidayPhase target, Context ctx, List<HolidayTransition> out) {
        HolidayPhase current = rec.phase;
        if (current == HolidayPhase.COMPLETED || target == current) return;
        if (target.ordinal() > current.ordinal()) {
            if (current == HolidayPhase.INACTIVE && ctx.policy() == SeasonSyncPolicy.REQUIRE_MATCHING_SEASON
                    && !h.prefersSeason(ctx.season())) {
                out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, current, HolidayPhase.COMPLETED,
                        HolidayTransition.Kind.SKIP, false, "season_mismatch"));
                rec.phase = HolidayPhase.COMPLETED;
                rec.skipReason = "season_mismatch";
                return;
            }
            if (current == HolidayPhase.INACTIVE && rec.activations > 0) {
                // Celebrated before a rewind: never prepare (or decorate) for it again.
                out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, current, HolidayPhase.COMPLETED,
                        HolidayTransition.Kind.ABORT, false, "already_celebrated"));
                rec.phase = HolidayPhase.COMPLETED;
                rec.skipReason = "already_celebrated";
                return;
            }
            HolidayPhase from = current;
            for (int p = current.ordinal() + 1; p <= target.ordinal(); p++) {
                HolidayPhase to = HolidayPhase.values()[p];
                if (to == HolidayPhase.ACTIVE) {
                    if (rec.activations > 0) {
                        // Already celebrated once (e.g. aborted after starting and re-prepared): never activate twice.
                        out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, from, HolidayPhase.COMPLETED,
                                HolidayTransition.Kind.ABORT, false, "already_celebrated"));
                        rec.phase = HolidayPhase.COMPLETED;
                        rec.skipReason = "already_celebrated";
                        return;
                    }
                    rec.activations++;
                }
                out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, from, to,
                        HolidayTransition.Kind.ENTER, p != target.ordinal(), ""));
                from = to;
            }
            rec.phase = target;
        } else if (target == HolidayPhase.INACTIVE && current.isLive()) {
            out.add(new HolidayTransition(rec.holidayId, rec.key(), rec.year, current, HolidayPhase.INACTIVE,
                    HolidayTransition.Kind.ABORT, false, "date_moved_back"));
            rec.phase = HolidayPhase.INACTIVE;
        }
        // Any other backward move (e.g. ACTIVE -> PREPARING after a small rewind) keeps the later phase.
    }

    /** Drops records older than {@code keepYears} that can no longer matter. */
    public static int prune(Map<String, OccurrenceRecord> records, int currentYear, int keepYears) {
        int before = records.size();
        records.values().removeIf(r -> !r.phase.isLive() && r.year < currentYear - keepYears);
        return before - records.size();
    }

    /** Display status for one holiday occurrence, using the persisted record when one exists. */
    public static HolidayPhase displayPhase(Map<String, OccurrenceRecord> records, ScheduledHoliday h, int year, Context ctx) {
        OccurrenceRecord rec = records.get(OccurrenceRecord.key(h.id(), ctx.system().signature(), year));
        if (rec != null) return rec.phase;
        HolidayPhase p = h.window(year, ctx.system()).phaseAt(ctx.today());
        return p.isLive() ? HolidayPhase.INACTIVE : p;
    }
}
