package com.holidayseasons.compat.sereneseasons;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.SubSeason;
import com.holidayseasons.seasons.SeasonAdapter;
import com.holidayseasons.seasons.SeasonReading;
import net.minecraft.server.level.ServerLevel;
import sereneseasons.api.SSGameRules;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;

import java.util.Optional;

/**
 * Reads Serene Seasons through its public API ({@code sereneseasons.api}) only. Never loaded unless the
 * mod is present. Serene Seasons exposes no API to change its cycle length, so EverFest adapts to it.
 */
public final class SereneSeasonsAdapter implements SeasonAdapter {
    private boolean loggedFailure;

    private SereneSeasonsAdapter() {
    }

    public static SereneSeasonsAdapter create() {
        // Touch the API classes now so incompatibilities surface at startup instead of mid-game.
        SubSeason.byOrdinal(sereneseasons.api.season.Season.SubSeason.EARLY_SPRING.ordinal());
        return new SereneSeasonsAdapter();
    }

    @Override
    public String name() {
        return "Serene Seasons";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<SeasonReading> read(ServerLevel level) {
        try {
            ISeasonState state = SeasonHelper.getSeasonState(level);
            if (state == null) return Optional.empty();
            long subTicks = state.getSubSeasonDuration();
            if (subTicks <= 0) return Optional.empty();
            boolean cycling = SSGameRules.RULE_DOSEASONCYCLE == null || level.getGameRules().getBoolean(SSGameRules.RULE_DOSEASONCYCLE);
            return Optional.of(new SeasonReading(
                    state.getSeasonCycleTicks(),
                    subTicks,
                    state.getDayDuration(),
                    SubSeason.byOrdinal(state.getSubSeason().ordinal()),
                    cycling));
        } catch (RuntimeException e) {
            if (!loggedFailure) {
                loggedFailure = true;
                EverFest.LOGGER.error("EverFest: failed to read Serene Seasons state; falling back to calendar seasons", e);
            }
            return Optional.empty();
        }
    }
}
