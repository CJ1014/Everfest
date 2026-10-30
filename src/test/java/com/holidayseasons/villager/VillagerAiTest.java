package com.holidayseasons.villager;

import com.holidayseasons.ai.ActivityPicker;
import com.holidayseasons.ai.TaskType;
import com.holidayseasons.decoration.DecorationSchedule;
import com.holidayseasons.holiday.HolidayPhase;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VillagerAiTest {
    static final Set<String> HALLOWEEN = Set.of("decorate", "gather", "socialize", "trick_or_treat", "hand_out_candy", "visit_homes");
    static final Set<String> CHRISTMAS = Set.of("decorate", "gather", "socialize", "gift_exchange", "feast");

    @Test
    void personalityIsDeterministicPerVillager() {
        UUID id = UUID.fromString("0f3c6a1e-2b7d-4f7e-9a51-3c2d1e0f9a8b");
        assertEquals(VillagerPersonality.of(id, "farmer", false), VillagerPersonality.of(id, "farmer", false));
        assertNotEquals(VillagerPersonality.of(id, "farmer", false), VillagerPersonality.of(UUID.randomUUID(), "farmer", false));
    }

    @Test
    void personalityTraitsInRangeAndVaried() {
        Random r = new Random(7);
        Map<VillagerPersonality.Costume, Integer> costumes = new EnumMap<>(VillagerPersonality.Costume.class);
        double babyEnthusiasm = 0, adultEnthusiasm = 0;
        for (int i = 0; i < 4000; i++) {
            UUID id = new UUID(r.nextLong(), r.nextLong());
            VillagerPersonality p = VillagerPersonality.of(id, "none", false);
            assertTrue(p.sociability() >= 0 && p.sociability() <= 1);
            assertTrue(p.generosity() >= 0 && p.generosity() <= 1);
            assertTrue(p.enthusiasm() >= 0.35f && p.enthusiasm() <= 1);
            costumes.merge(p.costume(), 1, Integer::sum);
            adultEnthusiasm += p.enthusiasm();
            babyEnthusiasm += VillagerPersonality.of(id, "none", true).enthusiasm();
        }
        assertEquals(VillagerPersonality.Costume.values().length, costumes.size(), "every costume occurs");
        assertTrue(babyEnthusiasm > adultEnthusiasm, "babies are more enthusiastic");
    }

    static ActivityPicker.Input input(HolidayPhase phase, Set<String> behaviors, boolean baby, String profession, UUID id) {
        return new ActivityPicker.Input(phase, behaviors, VillagerPersonality.of(id, profession, baby), profession, baby, 1.0, Set.of());
    }

    @Test
    void babiesTrickOrTreatAndAdultsDoNot() {
        Random r = new Random(1);
        Map<TaskType, Integer> baby = new EnumMap<>(TaskType.class);
        Map<TaskType, Integer> adult = new EnumMap<>(TaskType.class);
        for (int i = 0; i < 5000; i++) {
            UUID id = new UUID(r.nextLong(), r.nextLong());
            ActivityPicker.pick(input(HolidayPhase.ACTIVE, HALLOWEEN, true, "none", id), r).ifPresent(t -> baby.merge(t, 1, Integer::sum));
            ActivityPicker.pick(input(HolidayPhase.ACTIVE, HALLOWEEN, false, "none", id), r).ifPresent(t -> adult.merge(t, 1, Integer::sum));
        }
        assertTrue(baby.getOrDefault(TaskType.TRICK_OR_TREAT, 0) > baby.values().stream().mapToInt(Integer::intValue).sum() / 3,
                "trick-or-treating dominates babies' Halloween: " + baby);
        assertFalse(adult.containsKey(TaskType.TRICK_OR_TREAT));
        assertFalse(baby.containsKey(TaskType.HAND_OUT_CANDY));
        assertTrue(adult.containsKey(TaskType.HAND_OUT_CANDY));
        assertFalse(adult.containsKey(TaskType.GIFT_EXCHANGE), "only holidays with gift_exchange get gifts");
    }

    @Test
    void phaseDrivesTasks() {
        UUID id = new UUID(1, 2);
        Map<TaskType, Double> prep = ActivityPicker.weights(input(HolidayPhase.PREPARING, CHRISTMAS, false, "farmer", id));
        assertTrue(prep.containsKey(TaskType.DECORATE));
        assertFalse(prep.containsKey(TaskType.GIFT_EXCHANGE));
        Map<TaskType, Double> active = ActivityPicker.weights(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "farmer", id));
        assertTrue(active.containsKey(TaskType.GIFT_EXCHANGE));
        assertTrue(active.containsKey(TaskType.FEAST));
        assertFalse(active.containsKey(TaskType.DECORATE));
        Map<TaskType, Double> cleanup = ActivityPicker.weights(input(HolidayPhase.CLEANUP, CHRISTMAS, false, "farmer", id));
        assertTrue(cleanup.containsKey(TaskType.TIDY_UP));
        assertTrue(ActivityPicker.weights(input(HolidayPhase.INACTIVE, CHRISTMAS, false, "farmer", id)).isEmpty());
        assertTrue(ActivityPicker.weights(input(HolidayPhase.PREPARING, CHRISTMAS, true, "none", id)).keySet().stream().noneMatch(TaskType::adultsOnly));
    }

    @Test
    void professionsInfluenceWeights() {
        UUID id = new UUID(5, 6);
        double farmerFeast = ActivityPicker.weights(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "farmer", id)).get(TaskType.FEAST);
        double masonFeast = ActivityPicker.weights(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "mason", id)).get(TaskType.FEAST);
        assertTrue(farmerFeast > masonFeast);
        double clericGather = ActivityPicker.weights(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "cleric", id)).get(TaskType.GATHER);
        double masonGather = ActivityPicker.weights(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "mason", id)).get(TaskType.GATHER);
        assertTrue(clericGather > masonGather);
    }

    @Test
    void nitwitsParticipate() {
        Random r = new Random(3);
        int joined = 0;
        for (int i = 0; i < 1000; i++) {
            UUID id = new UUID(r.nextLong(), r.nextLong());
            Optional<TaskType> t = ActivityPicker.pick(input(HolidayPhase.ACTIVE, CHRISTMAS, false, "nitwit", id), r);
            if (t.isPresent()) joined++;
        }
        assertTrue(joined > 500, "nitwits are enthusiastic: " + joined);
    }

    @Test
    void zeroIntensityMeansNoParticipation() {
        Random r = new Random(9);
        ActivityPicker.Input in = new ActivityPicker.Input(HolidayPhase.ACTIVE, CHRISTMAS,
                VillagerPersonality.of(new UUID(1, 1), "none", false), "none", false, 0, Set.of());
        for (int i = 0; i < 200; i++) assertTrue(ActivityPicker.pick(in, r).isEmpty());
    }

    @Test
    void decorationTargetsRampAndExpiryIsStaggered() {
        assertEquals(24, DecorationSchedule.target(24, 1, true, 0, 4));
        int prev = 0;
        for (int d = 0; d < 4; d++) {
            int t = DecorationSchedule.target(24, 1, false, d, 4);
            assertTrue(t >= prev && t < 24);
            prev = t;
        }
        assertEquals(0, DecorationSchedule.target(24, 0, true, 0, 4));
        assertEquals(48, DecorationSchedule.target(24, 2, true, 0, 4));
        java.util.Set<Long> days = new java.util.HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            long e = DecorationSchedule.expiryDay(100, 3, seed);
            assertTrue(e >= 101 && e <= 103, "expiry " + e);
            days.add(e);
        }
        assertEquals(3, days.size(), "decorations disappear over all cleanup days");
        assertEquals(100, DecorationSchedule.expiryDay(100, 0, 5));
    }
}
