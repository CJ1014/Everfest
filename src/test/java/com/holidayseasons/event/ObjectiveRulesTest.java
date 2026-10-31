package com.holidayseasons.event;

import com.holidayseasons.home.PartyRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveRulesTest {
    @Test
    void objectivesAreStablePerVillageAndYear() {
        var a = ObjectiveRules.choose("holidayseasons:halloween", "holidayseasons:halloween|sig|7", "minecraft:overworld|bell|1,64,2");
        assertEquals(a, ObjectiveRules.choose("holidayseasons:halloween", "holidayseasons:halloween|sig|7", "minecraft:overworld|bell|1,64,2"));
        Set<String> seen = new HashSet<>();
        for (int v = 0; v < 40; v++) seen.add(ObjectiveRules.choose("holidayseasons:halloween", "occ|7", "village" + v).item());
        assertTrue(seen.size() > 1, "different villages ask for different things");
    }

    @Test
    void everyHolidayHasAnObjectiveAndUnknownOnesFallBack() {
        for (String h : new String[]{"halloween", "christmas", "easter", "valentines", "thanksgiving", "new_years", "independence_day", "my_pack:festival"}) {
            var o = ObjectiveRules.choose(h.contains(":") ? h : "holidayseasons:" + h, "occ", "v");
            assertTrue(o.needed() > 0 && !o.item().isEmpty(), h);
        }
    }

    @Test
    void neverTakesMoreThanNeeded() {
        assertEquals(10, ObjectiveRules.take(0, 24, 10));
        assertEquals(4, ObjectiveRules.take(20, 24, 64));
        assertEquals(0, ObjectiveRules.take(24, 24, 5));
        assertEquals(0, ObjectiveRules.take(30, 24, 5));
        assertEquals(0, ObjectiveRules.take(0, 24, 0));
    }

    @Test
    void partyInvitationsDependOnReputation() {
        assertFalse(PartyRules.accepts(-5, 0.0), "disliked hosts get no guests");
        assertTrue(PartyRules.accepts(0, 0.2));
        assertFalse(PartyRules.accepts(0, 0.5));
        assertTrue(PartyRules.accepts(20, 0.5));
        assertFalse(PartyRules.accepts(20, 0.9));
        assertTrue(PartyRules.accepts(60, 0.99), "friends always come");
    }
}
