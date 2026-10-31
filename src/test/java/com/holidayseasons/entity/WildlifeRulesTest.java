package com.holidayseasons.entity;

import com.holidayseasons.calendar.Season;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WildlifeRulesTest {
    @Test
    void speciesVisitInTheirOwnSeason() {
        assertEquals(List.of(SeasonalWildlife.Species.SPRING_BUNNY), SeasonalWildlife.inSeason(Season.SPRING, false, false));
        assertEquals(List.of(SeasonalWildlife.Species.WILD_TURKEY), SeasonalWildlife.inSeason(Season.AUTUMN, false, false));
        assertTrue(SeasonalWildlife.inSeason(Season.SUMMER, false, false).isEmpty());
        assertTrue(SeasonalWildlife.inSeason(Season.WINTER, false, false).isEmpty());
    }

    @Test
    void theirHolidayKeepsThemAroundOutOfSeason() {
        assertEquals(List.of(SeasonalWildlife.Species.SPRING_BUNNY), SeasonalWildlife.inSeason(Season.WINTER, true, false));
        assertEquals(List.of(SeasonalWildlife.Species.WILD_TURKEY), SeasonalWildlife.inSeason(Season.WINTER, false, true));
    }

    @Test
    void arrivalsNeverExceedTheCap() {
        assertFalse(SeasonalWildlife.mayArrive(5, 5, 0.0, 1.0, true), "full even when forced");
        assertFalse(SeasonalWildlife.mayArrive(0, 0, 0.0, 1.0, true), "cap 0 disables visits");
        assertTrue(SeasonalWildlife.mayArrive(4, 5, 0.9, 0.3, true), "forced ignores the roll");
        assertFalse(SeasonalWildlife.mayArrive(1, 5, 0.9, 0.3, false));
        assertTrue(SeasonalWildlife.mayArrive(1, 5, 0.1, 0.3, false));
        Random r = new Random(7);
        for (int nearby = 0; nearby <= 6; nearby++) {
            for (int i = 0; i < 50; i++) {
                int g = SeasonalWildlife.groupSize(SeasonalWildlife.Species.WILD_TURKEY, nearby, 5, r);
                assertTrue(g >= 0 && nearby + g <= Math.max(5, nearby), "nearby " + nearby + " + group " + g);
                assertTrue(g <= SeasonalWildlife.Species.WILD_TURKEY.maxGroup);
            }
        }
    }

    @Test
    void outfitsOnlyDuringTheirHoliday() {
        UUID id = UUID.randomUUID();
        assertNull(FestiveOutfits.outfitFor("minecraft:sheep", id, Set.of()));
        assertNull(FestiveOutfits.outfitFor("minecraft:pig", id, Set.of("holidayseasons:christmas")));
        assertNull(FestiveOutfits.outfitFor("minecraft:sheep", id, Set.of("holidayseasons:halloween")));
    }

    @Test
    void outfitsAreStablePerAnimalAndRoughlyTheConfiguredShare() {
        Random r = new Random(42);
        int scarves = 0;
        int crowns = 0;
        int n = 6000;
        for (int i = 0; i < n; i++) {
            UUID id = new UUID(r.nextLong(), r.nextLong());
            var christmas = FestiveOutfits.outfitFor("minecraft:sheep", id, Set.of("holidayseasons:christmas"));
            assertEquals(christmas, FestiveOutfits.outfitFor("minecraft:sheep", id, Set.of("holidayseasons:christmas")), "stable");
            if (christmas == FestiveOutfits.Outfit.SCARF) scarves++;
            if (FestiveOutfits.outfitFor("minecraft:rabbit", id, Set.of("holidayseasons:easter")) == FestiveOutfits.Outfit.FLOWER_CROWN) crowns++;
        }
        assertEquals(n / 3.0, scarves, n * 0.03, "about a third of sheep wear scarves");
        assertEquals(n / 2.0, crowns, n * 0.03, "about half of rabbits wear crowns");
    }
}
