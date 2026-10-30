package com.holidayseasons.holiday;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.holiday.rule.EasterRule;
import com.holidayseasons.holiday.rule.NthWeekdayRule;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HolidayDefinitionTest {
    static final String[] BUILT_IN = {"halloween", "christmas", "easter", "new_years", "valentines", "thanksgiving", "independence_day"};

    static HolidayDefinition load(String name) throws Exception {
        try (InputStream in = HolidayDefinitionTest.class.getResourceAsStream("/data/holidayseasons/everfest/holiday/" + name + ".json")) {
            assertNotNull(in, "missing holiday json " + name);
            JsonElement json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return HolidayDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(msg -> new AssertionError(name + ": " + msg));
        }
    }

    @Test
    void allBuiltInHolidaysParse() throws Exception {
        for (String name : BUILT_IN) {
            HolidayDefinition d = load(name);
            assertTrue(d.durationDays() >= 1, name);
            assertFalse(d.behaviors().isEmpty(), name + " has behaviours");
            assertFalse(d.seasons().isEmpty(), name + " has season preferences");
        }
    }

    @Test
    void builtInSpecifics() throws Exception {
        assertEquals("fixed:10-31", load("halloween").date().describe());
        assertEquals(java.util.List.of(Season.AUTUMN), load("halloween").seasons());
        assertInstanceOf(EasterRule.class, load("easter").date());
        assertInstanceOf(NthWeekdayRule.class, load("thanksgiving").date());
        assertFalse(load("independence_day").enabled(), "Independence Day is opt-in");
        assertTrue(load("halloween").hasBehavior("trick_or_treat"));
        assertTrue(load("christmas").hasBehavior("gift_exchange"));
        assertEquals(0xFFE8751A, load("halloween").color());
    }

    @Test
    void invalidDefinitionsAreRejected() {
        JsonElement bad = JsonParser.parseString("{\"date\": \"fixed:2-31\"}");
        assertTrue(HolidayDefinition.CODEC.parse(JsonOps.INSTANCE, bad).error().isPresent());
        JsonElement badDuration = JsonParser.parseString("{\"date\": \"fixed:2-14\", \"duration_days\": 0}");
        assertTrue(HolidayDefinition.CODEC.parse(JsonOps.INSTANCE, badDuration).error().isPresent());
        JsonElement minimal = JsonParser.parseString("{\"date\": \"native:5-5\"}");
        HolidayDefinition d = HolidayDefinition.CODEC.parse(JsonOps.INSTANCE, minimal).getOrThrow();
        assertEquals(1, d.durationDays());
        assertTrue(d.enabled());
    }
}
