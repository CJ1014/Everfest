package com.holidayseasons.reputation;

/** Pure reputation rules, shared by the server and unit tests. */
public final class ReputationMath {
    public static final int MAX = 1000;
    public static final int MIN = -200;

    public enum Action {
        GAVE_CANDY(2), GAVE_PRESENT(4), FESTIVAL_PARTICIPATION(1), HELPED_VILLAGER(2), COMMUNITY_OBJECTIVE(10), TRICK_PLAYED_ON(-1);

        public final int points;

        Action(int points) {
            this.points = points;
        }
    }

    public enum Tier {
        STRANGER(0), NEIGHBOUR(15), FRIEND(50), CHERISHED(120), LEGEND(300);

        public final int threshold;

        Tier(int threshold) {
            this.threshold = threshold;
        }
    }

    private ReputationMath() {
    }

    /**
     * Applies an action with diminishing returns: the same action repeated many times on one day earns less each
     * time, so reputation cannot be farmed with an infinite loop.
     */
    public static int apply(int current, Action action, int sameActionToday) {
        int points = action.points;
        if (points > 0) {
            if (sameActionToday >= 12) points = 0;
            else if (sameActionToday >= 6) points = Math.max(1, points / 4);
            else if (sameActionToday >= 3) points = Math.max(1, points / 2);
        }
        return Math.max(MIN, Math.min(MAX, current + points));
    }

    public static Tier tier(int reputation) {
        Tier result = Tier.STRANGER;
        for (Tier t : Tier.values()) if (reputation >= t.threshold) result = t;
        return result;
    }
}
