package com.holidayseasons.reputation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReputationMathTest {
    @Test
    void actionsAddPoints() {
        assertEquals(2, ReputationMath.apply(0, ReputationMath.Action.GAVE_CANDY, 0));
        assertEquals(4, ReputationMath.apply(0, ReputationMath.Action.GAVE_PRESENT, 0));
        assertEquals(-1, ReputationMath.apply(0, ReputationMath.Action.TRICK_PLAYED_ON, 0));
    }

    @Test
    void repeatingAnActionTheSameDayHasDiminishingReturns() {
        int rep = 0;
        int[] gains = new int[20];
        for (int i = 0; i < 20; i++) {
            int next = ReputationMath.apply(rep, ReputationMath.Action.GAVE_PRESENT, i);
            gains[i] = next - rep;
            rep = next;
        }
        assertEquals(4, gains[0]);
        assertEquals(2, gains[3]);
        assertEquals(1, gains[6]);
        assertEquals(0, gains[12], "farming stops paying after a dozen repeats");
        assertTrue(rep < 4 * 20 / 2, "total is far below the naive sum: " + rep);
        for (int i = 1; i < 20; i++) assertTrue(gains[i] <= gains[i - 1]);
    }

    @Test
    void boundedAndTiered() {
        assertEquals(ReputationMath.MAX, ReputationMath.apply(ReputationMath.MAX, ReputationMath.Action.COMMUNITY_OBJECTIVE, 0));
        assertEquals(ReputationMath.MIN, ReputationMath.apply(ReputationMath.MIN, ReputationMath.Action.TRICK_PLAYED_ON, 0));
        assertEquals(ReputationMath.Tier.STRANGER, ReputationMath.tier(0));
        assertEquals(ReputationMath.Tier.NEIGHBOUR, ReputationMath.tier(15));
        assertEquals(ReputationMath.Tier.FRIEND, ReputationMath.tier(60));
        assertEquals(ReputationMath.Tier.LEGEND, ReputationMath.tier(999));
        assertEquals(ReputationMath.Tier.STRANGER, ReputationMath.tier(-50));
    }
}
