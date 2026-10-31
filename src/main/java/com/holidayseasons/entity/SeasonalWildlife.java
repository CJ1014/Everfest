package com.holidayseasons.entity;

import com.holidayseasons.calendar.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure rules for seasonal animal visits: which species are in season and how many may arrive. Kept free of game
 * classes so they can be unit-tested.
 */
public final class SeasonalWildlife {
    public enum Species {
        SPRING_BUNNY("spring_bunny", Season.SPRING, 1, 3),
        WILD_TURKEY("wild_turkey", Season.AUTUMN, 2, 4);

        public final String id;
        public final Season season;
        public final int minGroup;
        public final int maxGroup;

        Species(String id, Season season, int minGroup, int maxGroup) {
            this.id = id;
            this.season = season;
            this.minGroup = minGroup;
            this.maxGroup = maxGroup;
        }
    }

    private SeasonalWildlife() {
    }

    /**
     * Species visiting now. The calendar season decides, and a species' own holiday (Easter for bunnies, Thanksgiving
     * for turkeys) keeps it around even if that holiday falls just outside its season.
     */
    public static List<Species> inSeason(Season season, boolean easterTime, boolean thanksgivingTime) {
        List<Species> out = new ArrayList<>();
        for (Species s : Species.values()) {
            boolean holiday = (s == Species.SPRING_BUNNY && easterTime) || (s == Species.WILD_TURKEY && thanksgivingTime);
            if (s.season == season || holiday) out.add(s);
        }
        return out;
    }

    /** Whether a group may arrive: below the cap and the roll succeeds (forced visits ignore the roll, never the cap). */
    public static boolean mayArrive(int nearby, int cap, double roll, double chance, boolean force) {
        if (cap <= 0 || nearby >= cap) return false;
        return force || roll < chance;
    }

    /** Group size for an arrival, never pushing the local population past the cap. */
    public static int groupSize(Species s, int nearby, int cap, Random random) {
        int wanted = s.minGroup + random.nextInt(s.maxGroup - s.minGroup + 1);
        return Math.max(0, Math.min(wanted, cap - nearby));
    }
}
