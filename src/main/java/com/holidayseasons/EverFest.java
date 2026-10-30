package com.holidayseasons;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EverFest.MOD_ID)
public final class EverFest {
    public static final String MOD_ID = "holidayseasons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EverFest(IEventBus modBus, ModContainer container) {
        LOGGER.info("EverFest initializing");
    }
}
