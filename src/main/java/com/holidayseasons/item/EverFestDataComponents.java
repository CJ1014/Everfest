package com.holidayseasons.item;

import com.holidayseasons.EverFest;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EverFestDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, EverFest.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PresentInfo>> PRESENT = COMPONENTS.registerComponentType("present",
            b -> b.persistent(PresentInfo.CODEC).networkSynchronized(PresentInfo.STREAM_CODEC));

    private EverFestDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
