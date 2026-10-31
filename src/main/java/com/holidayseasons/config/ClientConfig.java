package com.holidayseasons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings. Never read on a dedicated server. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ClientConfig INSTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ClientConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.BooleanValue showDateHud;
    public final ModConfigSpec.BooleanValue use24HourClock;
    public final ModConfigSpec.BooleanValue showHolidayToasts;
    public final ModConfigSpec.DoubleValue effectsIntensity;
    public final ModConfigSpec.BooleanValue halloweenFog;
    public final ModConfigSpec.BooleanValue seasonalParticles;
    public final ModConfigSpec.BooleanValue ambientSounds;
    public final ModConfigSpec.BooleanValue festiveAnimalOutfits;
    public final ModConfigSpec.BooleanValue holidayMusic;

    private ClientConfig(ModConfigSpec.Builder b) {
        b.comment("Heads-up display").push("hud");
        showDateHud = b.comment("Show the current date and next holiday in the top-left corner.")
                .translation("holidayseasons.configuration.showDateHud")
                .define("showDateHud", false);
        use24HourClock = b.translation("holidayseasons.configuration.use24HourClock")
                .define("use24HourClock", false);
        showHolidayToasts = b.comment("Show a notification when a holiday begins preparing or starts.")
                .translation("holidayseasons.configuration.showHolidayToasts")
                .define("showHolidayToasts", true);
        b.pop();

        b.comment("Atmosphere and visual effects (client-side only).").push("effects");
        effectsIntensity = b.comment("Scales particles, fog and ambient sounds. 0 disables them.")
                .translation("holidayseasons.configuration.effectsIntensity").defineInRange("intensity", 1.0, 0.0, 2.0);
        halloweenFog = b.comment("Creepy fog on Halloween nights.")
                .translation("holidayseasons.configuration.halloweenFog").define("halloweenFog", true);
        seasonalParticles = b.comment("Falling leaves in autumn and other seasonal particles.")
                .translation("holidayseasons.configuration.seasonalParticles").define("seasonalParticles", true);
        ambientSounds = b.comment("Distant bells, wind and whispers on Halloween nights.")
                .translation("holidayseasons.configuration.ambientSounds").define("ambientSounds", true);
        festiveAnimalOutfits = b.comment("Some sheep, cows, wolves and rabbits dress up for the holidays (purely cosmetic).")
                .translation("holidayseasons.configuration.festiveAnimalOutfits").define("festiveAnimalOutfits", true);
        holidayMusic = b.comment("While a holiday is celebrated, EverFest's original holiday music replaces the overworld background music.",
                        "Its volume follows the Music slider.")
                .translation("holidayseasons.configuration.holidayMusic").define("holidayMusic", true);
        b.pop();
    }
}
