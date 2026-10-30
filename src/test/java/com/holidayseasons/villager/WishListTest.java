package com.holidayseasons.villager;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WishListTest {
    @Test
    void deterministicDistinctAndFromCatalogue() {
        UUID id = new UUID(42, 7);
        for (VillagerPersonality.GiftPreference pref : VillagerPersonality.GiftPreference.values()) {
            List<String> a = WishList.of(id, pref, 3);
            assertEquals(a, WishList.of(id, pref, 3), "same villager, same year, same wishes");
            assertEquals(3, a.size());
            assertEquals(3, new HashSet<>(a).size(), "no duplicate wishes");
            assertTrue(WishList.CATALOGUE.get(pref).containsAll(a));
        }
    }

    @Test
    void wishesChangeBetweenYearsAndVillagers() {
        UUID id = new UUID(1, 2);
        int differentYears = 0;
        for (int y = 1; y < 10; y++) {
            if (!WishList.of(id, VillagerPersonality.GiftPreference.FOOD, y).equals(WishList.of(id, VillagerPersonality.GiftPreference.FOOD, y + 1))) differentYears++;
        }
        assertTrue(differentYears >= 5, "wish lists vary from year to year");
    }

    @Test
    void appreciation() {
        List<String> wishes = List.of("minecraft:cake", "minecraft:cookie", "minecraft:honey_bottle");
        var food = VillagerPersonality.GiftPreference.FOOD;
        assertEquals(2, WishList.appreciation(wishes, food, List.of("minecraft:dirt", "minecraft:cake")));
        assertEquals(1, WishList.appreciation(wishes, food, List.of("minecraft:pumpkin_pie")));
        assertEquals(0, WishList.appreciation(wishes, food, List.of("minecraft:dirt")));
        assertEquals(0, WishList.appreciation(wishes, food, List.of()));
    }
}
