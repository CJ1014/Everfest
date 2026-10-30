package com.holidayseasons.seasons;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/** Read-only bridge to a seasonal mod. EverFest never writes another mod's internal state through this. */
public interface SeasonAdapter {
    String name();

    boolean isAvailable();

    /** Current season state for a level, or empty if the provider has no seasons there. */
    Optional<SeasonReading> read(ServerLevel level);

    SeasonAdapter NONE = new SeasonAdapter() {
        @Override
        public String name() {
            return "none";
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Optional<SeasonReading> read(ServerLevel level) {
            return Optional.empty();
        }
    };
}
