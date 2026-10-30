package com.holidayseasons.block;

import com.holidayseasons.EverFest;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EverFestBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EverFest.MOD_ID);

    public static final DeferredBlock<WallCalendarBlock> WALL_CALENDAR = BLOCKS.register("wall_calendar",
            () -> new WallCalendarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL).strength(0.2f).sound(SoundType.WOOD)
                    .noCollission().pushReaction(PushReaction.DESTROY)));

    private EverFestBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
