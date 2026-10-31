package com.holidayseasons.reputation;

import com.holidayseasons.EverFest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Grants EverFest's gameplay advancements (data: data/holidayseasons/advancement/everfest/*.json, "done" criterion). */
public final class EverFestAdvancements {
    public static final String TRICK_OR_TREAT = "trick_or_treat";
    public static final String HOME_SWEET_HOME = "home_sweet_home";
    public static final String WELL_CHOSEN = "well_chosen";
    public static final String EGG_HUNTER = "egg_hunter";
    public static final String HAPPY_NEW_YEAR = "happy_new_year";
    public static final String COMMUNITY = "pillar_of_the_community";
    public static final String LIFE_OF_THE_PARTY = "life_of_the_party";
    public static final String THE_PEDDLER = "the_peddler";
    public static final String RESTLESS_GRAVES = "restless_graves";
    public static final String RED_NOSED = "red_nosed";
    public static final String LOCAL_LEGEND = "local_legend";
    public static final String COLLECTOR = "collector";
    public static final String YEAR_OF_FESTIVALS = "a_year_of_festivals";

    private EverFestAdvancements() {
    }

    /** Awards {@code id} to {@code player} if it is a real server player and does not have it yet. Returns true if newly done. */
    public static boolean award(Player player, String id) {
        if (!(player instanceof ServerPlayer sp) || sp.getServer() == null) return false;
        AdvancementHolder adv = sp.getServer().getAdvancements().get(EverFest.id("everfest/" + id));
        if (adv == null) {
            EverFest.LOGGER.warn("EverFest: unknown advancement {}", id);
            return false;
        }
        if (sp.getAdvancements().getOrStartProgress(adv).isDone()) return false;
        return sp.getAdvancements().award(adv, "done");
    }
}
