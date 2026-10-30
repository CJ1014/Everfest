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
    }
}
