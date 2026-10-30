package com.holidayseasons.ai;

import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.villager.VillagerPersonality;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Chooses what a villager does next during a holiday. Pure and deterministic for a given {@link Random} so the
 * weighting can be unit tested.
 */
public final class ActivityPicker {
    private ActivityPicker() {
    }

    public record Input(HolidayPhase phase, Set<String> behaviors, VillagerPersonality personality, String profession,
                        boolean baby, double intensity, Set<TaskType> unavailable) {
    }

    /** Probability that a villager takes part in anything when asked. */
    public static double participationChance(Input in) {
        double phaseFactor = switch (in.phase()) {
            case PREPARING -> 0.55;
            case ACTIVE -> 0.9;
            case CLEANUP -> 0.4;
            default -> 0;
        };
        return Math.max(0, Math.min(1, in.personality().enthusiasm() * phaseFactor * in.intensity()));
    }

    public static Map<TaskType, Double> weights(Input in) {
        Map<TaskType, Double> w = new EnumMap<>(TaskType.class);
        VillagerPersonality p = in.personality();
        switch (in.phase()) {
            case PREPARING -> {
                put(w, in, TaskType.DECORATE, 4);
                put(w, in, TaskType.HIDE_EGGS, 3);
                put(w, in, TaskType.SOCIALIZE, 1);
            }
            case ACTIVE -> {
                put(w, in, TaskType.GATHER, 3);
                put(w, in, TaskType.SOCIALIZE, 2);
                put(w, in, TaskType.FEAST, 2.5);
                put(w, in, TaskType.GIFT_EXCHANGE, 3);
                put(w, in, TaskType.FIREWORKS, 1.2);
                put(w, in, TaskType.TRICK_OR_TREAT, 8);
                put(w, in, TaskType.HAND_OUT_CANDY, 2.5);
                put(w, in, TaskType.EGG_HUNT, 8);
                put(w, in, TaskType.VISIT_HOME, 1.5);
            }
            case CLEANUP -> {
                // Whoever decorates also tidies up afterwards.
                if (in.behaviors().contains("decorate")) w.put(TaskType.TIDY_UP, 3.0);
                put(w, in, TaskType.SOCIALIZE, 1);
            }
            default -> {
                return w;
            }
        }
        // Personality.
        scale(w, TaskType.SOCIALIZE, 0.5 + p.sociability());
        scale(w, TaskType.GATHER, 0.5 + p.sociability());
        scale(w, TaskType.GIFT_EXCHANGE, 0.4 + p.generosity() * 1.2);
        scale(w, TaskType.HAND_OUT_CANDY, 0.4 + p.generosity() * 1.2);
        switch (p.preferred()) {
            case DECORATE -> scale(w, TaskType.DECORATE, 2);
            case GATHER -> scale(w, TaskType.GATHER, 2);
            case SOCIALIZE -> scale(w, TaskType.SOCIALIZE, 2);
            case FEAST -> scale(w, TaskType.FEAST, 2);
            case GIFTS -> scale(w, TaskType.GIFT_EXCHANGE, 2);
            case GAMES -> {
                scale(w, TaskType.EGG_HUNT, 1.5);
                scale(w, TaskType.TRICK_OR_TREAT, 1.5);
                scale(w, TaskType.GATHER, 1.3);
            }
        }
        // Profession.
        switch (in.profession()) {
            case "farmer" -> {
                scale(w, TaskType.FEAST, 1.8);
                scale(w, TaskType.DECORATE, 1.4);
            }
            case "cleric" -> scale(w, TaskType.GATHER, 2);
            case "librarian" -> scale(w, TaskType.GIFT_EXCHANGE, 1.5);
            case "fisherman", "butcher" -> scale(w, TaskType.FEAST, 1.5);
            case "nitwit" -> scale(w, TaskType.GATHER, 1.6);
            case "shepherd" -> scale(w, TaskType.DECORATE, 1.5);
            default -> {
            }
        }
        // Babies: prefer games, don't do adult jobs; adults leave the kids' games to the kids.
        if (in.baby()) {
            w.keySet().removeIf(TaskType::adultsOnly);
            w.remove(TaskType.DECORATE);
            w.remove(TaskType.TIDY_UP);
            scale(w, TaskType.GATHER, 1.2);
        } else {
            w.remove(TaskType.TRICK_OR_TREAT);
            w.remove(TaskType.EGG_HUNT);
        }
        for (TaskType t : in.unavailable()) w.remove(t);
        w.values().removeIf(v -> v <= 0);
        return w;
    }

    public static Optional<TaskType> pick(Input in, Random random) {
        if (random.nextDouble() >= participationChance(in)) return Optional.empty();
        Map<TaskType, Double> w = weights(in);
        double total = 0;
        for (double v : w.values()) total += v;
        if (total <= 0) return Optional.empty();
        double roll = random.nextDouble() * total;
        for (Map.Entry<TaskType, Double> e : w.entrySet()) {
            roll -= e.getValue();
            if (roll < 0) return Optional.of(e.getKey());
        }
        return Optional.empty();
    }

    private static void put(Map<TaskType, Double> w, Input in, TaskType t, double weight) {
        if (in.behaviors().contains(t.behavior())) w.put(t, weight);
    }

    private static void scale(Map<TaskType, Double> w, TaskType t, double f) {
        w.computeIfPresent(t, (k, v) -> v * f);
    }
}
