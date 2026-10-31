package com.holidayseasons.home;

import com.holidayseasons.reputation.ReputationMath;

/** Who comes to a player's party. Pure rules so they can be unit-tested. */
public final class PartyRules {
    private PartyRules() {
    }

    /**
     * Whether a villager accepts an invitation from a host with {@code reputation} in the villager's village:
     * never if the host is disliked, sometimes as a stranger, usually as a neighbour, always as a friend.
     */
    public static boolean accepts(int reputation, double roll) {
        if (reputation < 0) return false;
        return switch (ReputationMath.tier(reputation)) {
            case STRANGER -> roll < 0.35;
            case NEIGHBOUR -> roll < 0.7;
            default -> true;
        };
    }
}
