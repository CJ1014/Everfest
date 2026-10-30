package com.holidayseasons.holiday;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarSystem;
import com.holidayseasons.calendar.FixedMonthCalendarSystem;
import com.holidayseasons.calendar.GregorianCalendarSystem;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.calendar.SeasonSyncPolicy;
import com.holidayseasons.calendar.SubSeason;
import com.holidayseasons.holiday.rule.DateRule;
import com.holidayseasons.holiday.rule.FixedDateRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HolidayEngineTest {
    static final FixedMonthCalendarSystem SS = FixedMonthCalendarSystem.uniform(8, 2000);

    static ScheduledHoliday holiday(String id, DateRule rule, int prep, int dur, int clean, Season... seasons) {
        return new ScheduledHoliday(id, rule, prep, dur, clean, true, List.of(seasons), 0xFFFFFFFF, null);
    }

    static final ScheduledHoliday HALLOWEEN = holiday("t:halloween", new FixedDateRule(10, 31), 4, 1, 3, Season.AUTUMN);
    static final ScheduledHoliday NEW_YEAR = holiday("t:new_year", new FixedDateRule(12, 31), 2, 2, 2, Season.WINTER);

    static HolidayEngineCore.Context ctx(CalendarSystem sys, long day, SeasonSyncPolicy policy) {
        CalendarDate d = sys.fromEpochDay(day);
        return new HolidayEngineCore.Context(sys, day, d.year(), SubSeason.forMonth(d.month()).season(), policy);
    }

    static List<HolidayTransition> run(Map<String, OccurrenceRecord> recs, List<ScheduledHoliday> hs, CalendarSystem sys, long day) {
        return HolidayEngineCore.reconcile(recs, hs, ctx(sys, day, SeasonSyncPolicy.FOLLOW_DATE));
    }

    @Test
    void windowPhases() {
        HolidayWindow w = HALLOWEEN.window(1, SS);
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        assertEquals(start, w.start());
        assertEquals(HolidayPhase.INACTIVE, w.phaseAt(start - 5));
        assertEquals(HolidayPhase.PREPARING, w.phaseAt(start - 4));
        assertEquals(HolidayPhase.ACTIVE, w.phaseAt(start));
        assertEquals(HolidayPhase.CLEANUP, w.phaseAt(start + 1));
        assertEquals(HolidayPhase.COMPLETED, w.phaseAt(start + 4));
    }

    @Test
    void dayByDayProgressionEmitsEachPhaseOnce() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        List<HolidayTransition> all = new ArrayList<>();
        for (long d = 0; d < SS.yearLength(1) * 2L; d++) {
            all.addAll(run(recs, List.of(HALLOWEEN), SS, d));
            assertTrue(run(recs, List.of(HALLOWEEN), SS, d).isEmpty(), "idempotent on day " + d);
        }
        List<HolidayTransition> year1 = all.stream().filter(t -> t.year() == 1).toList();
        assertEquals(List.of(HolidayPhase.PREPARING, HolidayPhase.ACTIVE, HolidayPhase.CLEANUP, HolidayPhase.COMPLETED),
                year1.stream().map(HolidayTransition::to).toList());
        assertTrue(year1.stream().noneMatch(HolidayTransition::transientStep));
        assertEquals(2, all.stream().filter(t -> t.to() == HolidayPhase.ACTIVE).count(), "once per year");
    }

    @Test
    void jumpOverHolidayDoesNotCreateIt() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        run(recs, List.of(HALLOWEEN), SS, SS.toEpochDay(new CalendarDate(1, 9, 1)));
        List<HolidayTransition> t = run(recs, List.of(HALLOWEEN), SS, SS.toEpochDay(new CalendarDate(1, 12, 1)));
        assertTrue(t.isEmpty(), "jumping completely past an occurrence never starts it");
    }

    @Test
    void jumpIntoActiveEmitsTransientPreparation() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        List<HolidayTransition> t = run(recs, List.of(HALLOWEEN), SS, start);
        assertEquals(2, t.size());
        assertEquals(HolidayPhase.PREPARING, t.get(0).to());
        assertTrue(t.get(0).transientStep());
        assertEquals(HolidayPhase.ACTIVE, t.get(1).to());
        assertFalse(t.get(1).transientStep());
    }

    @Test
    void rewindAfterCompletionNeverCelebratesAgain() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        for (long d = start - 6; d < start + 6; d++) run(recs, List.of(HALLOWEEN), SS, d);
        assertEquals(HolidayPhase.COMPLETED, recs.values().iterator().next().phase());
        // Rewind into the middle of the holiday and replay it.
        for (long d = start - 2; d < start + 6; d++) {
            assertTrue(run(recs, List.of(HALLOWEEN), SS, d).isEmpty(), "no transitions replaying day " + d);
        }
    }

    @Test
    void rewindBeforePreparationAbortsAndCanPrepareAgainButActivatesOnce() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        run(recs, List.of(HALLOWEEN), SS, start - 2);
        List<HolidayTransition> abort = run(recs, List.of(HALLOWEEN), SS, start - 10);
        assertEquals(1, abort.size());
        assertEquals(HolidayTransition.Kind.ABORT, abort.get(0).kind());
        assertEquals(HolidayPhase.INACTIVE, abort.get(0).to());
        run(recs, List.of(HALLOWEEN), SS, start - 3);
        List<HolidayTransition> act = run(recs, List.of(HALLOWEEN), SS, start);
        assertEquals(HolidayPhase.ACTIVE, act.get(act.size() - 1).to());
        // Abort after activation, then come back: must not prepare or activate a second time.
        run(recs, List.of(HALLOWEEN), SS, start - 10);
        List<HolidayTransition> again = run(recs, List.of(HALLOWEEN), SS, start - 2);
        assertEquals(1, again.size());
        assertEquals("already_celebrated", again.get(0).reason());
        assertTrue(again.stream().noneMatch(t -> t.to() == HolidayPhase.ACTIVE || t.to() == HolidayPhase.PREPARING));
        assertTrue(run(recs, List.of(HALLOWEEN), SS, start).isEmpty());
        assertEquals(1, recs.get(OccurrenceRecord.key("t:halloween", SS.signature(), 1)).activations());
    }

    @Test
    void occurrenceSpanningYearBoundary() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long dec31 = SS.toEpochDay(new CalendarDate(1, 12, 8));
        List<HolidayTransition> all = new ArrayList<>();
        for (long d = dec31 - 5; d < dec31 + 6; d++) all.addAll(run(recs, List.of(NEW_YEAR), SS, d));
        assertEquals(List.of(HolidayPhase.PREPARING, HolidayPhase.ACTIVE, HolidayPhase.CLEANUP, HolidayPhase.COMPLETED),
                all.stream().map(HolidayTransition::to).toList());
        assertTrue(all.stream().allMatch(t -> t.year() == 1), "keyed by the year the holiday starts in");
    }

    @Test
    void disablingLiveHolidayAbortsIt() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        run(recs, List.of(HALLOWEEN), SS, start);
        ScheduledHoliday disabled = new ScheduledHoliday(HALLOWEEN.id(), HALLOWEEN.rule(), 4, 1, 3, false, List.of(), 0, null);
        List<HolidayTransition> t = run(recs, List.of(disabled), SS, start);
        assertEquals(1, t.size());
        assertEquals(HolidayTransition.Kind.ABORT, t.get(0).kind());
        assertEquals("disabled", t.get(0).reason());
    }

    @Test
    void calendarSystemChangeAbortsLiveOccurrence() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        run(recs, List.of(HALLOWEEN), SS, start);
        GregorianCalendarSystem g = GregorianCalendarSystem.INSTANCE;
        List<HolidayTransition> t = run(recs, List.of(HALLOWEEN), g, g.toEpochDay(new CalendarDate(2026, 3, 1)));
        assertEquals(1, t.size());
        assertEquals("calendar_changed", t.get(0).reason());
    }

    @Test
    void requireMatchingSeasonSkips() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        long start = SS.toEpochDay(new CalendarDate(1, 10, 8));
        HolidayEngineCore.Context c = new HolidayEngineCore.Context(SS, start - 3, 1, Season.SPRING, SeasonSyncPolicy.REQUIRE_MATCHING_SEASON);
        List<HolidayTransition> t = HolidayEngineCore.reconcile(recs, List.of(HALLOWEEN), c);
        assertEquals(1, t.size());
        assertEquals(HolidayTransition.Kind.SKIP, t.get(0).kind());
        HolidayEngineCore.Context c2 = new HolidayEngineCore.Context(SS, start, 1, Season.AUTUMN, SeasonSyncPolicy.REQUIRE_MATCHING_SEASON);
        assertTrue(HolidayEngineCore.reconcile(recs, List.of(HALLOWEEN), c2).isEmpty(), "skipped occurrence stays skipped");
    }

    @Test
    void randomTimeTravelNeverActivatesAnOccurrenceTwice() {
        Random rnd = new Random(1234);
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        List<ScheduledHoliday> hs = List.of(HALLOWEEN, NEW_YEAR,
                holiday("t:valentine", new FixedDateRule(2, 14), 3, 1, 2),
                holiday("t:long", new FixedDateRule(6, 1), 5, 6, 5));
        Map<String, Integer> activations = new HashMap<>();
        long day = 0;
        for (int step = 0; step < 20000; step++) {
            int r = rnd.nextInt(100);
            if (r < 80) day += 1;
            else if (r < 90) day += rnd.nextInt(40);
            else day = Math.max(0, day - rnd.nextInt(30));
            for (HolidayTransition t : run(recs, hs, SS, day)) {
                if (t.to() == HolidayPhase.ACTIVE) activations.merge(t.occurrenceKey(), 1, Integer::sum);
            }
        }
        assertFalse(activations.isEmpty());
        activations.forEach((k, v) -> assertEquals(1, v, "occurrence " + k + " activated " + v + " times"));
        recs.values().forEach(rec -> assertTrue(rec.activations() <= 1));
    }

    @Test
    void tinyCalendarClampsLongWindows() {
        FixedMonthCalendarSystem tiny = FixedMonthCalendarSystem.uniform(1, 2000);
        ScheduledHoliday big = holiday("t:big", new FixedDateRule(6, 1), 30, 30, 30);
        HolidayWindow w = big.window(1, tiny);
        assertTrue(w.cleanupEnd() - w.prepStart() <= tiny.yearLength(1));
    }

    @Test
    void pruneKeepsRecentAndLive() {
        Map<String, OccurrenceRecord> recs = new HashMap<>();
        for (long d = 0; d < SS.yearLength(1) * 6L; d++) run(recs, List.of(HALLOWEEN), SS, d);
        int removed = HolidayEngineCore.prune(recs, 6, 3);
        assertTrue(removed > 0);
        assertTrue(recs.values().stream().allMatch(r -> r.year >= 3));
    }
}
