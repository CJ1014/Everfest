package com.holidayseasons.block.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.EverFestBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EverFestBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EverFest.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CandyBowlBlockEntity>> CANDY_BOWL = TYPES.register("candy_bowl",
            () -> BlockEntityType.Builder.of(CandyBowlBlockEntity::new, EverFestBlocks.CANDY_BOWL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HearthBlockEntity>> HEARTH = TYPES.register("holiday_hearth",
            () -> BlockEntityType.Builder.of(HearthBlockEntity::new, EverFestBlocks.HOLIDAY_HEARTH.get()).build(null));

    private EverFestBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent e) ->
                e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CANDY_BOWL.get(), (be, side) -> be.automationHandler()));
    }
}
