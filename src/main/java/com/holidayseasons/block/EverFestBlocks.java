package com.holidayseasons.block;

import com.holidayseasons.EverFest;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class EverFestBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EverFest.MOD_ID);

    public static final DeferredBlock<WallCalendarBlock> WALL_CALENDAR = BLOCKS.register("wall_calendar",
            () -> new WallCalendarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL).strength(0.2f).sound(SoundType.WOOD)
                    .noCollission().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<CandyBowlBlock> CANDY_BOWL = BLOCKS.register("candy_bowl",
            () -> new CandyBowlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6f)
                    .sound(SoundType.DECORATED_POT).noOcclusion()));

    public static final DeferredBlock<DoorbellBlock> DOORBELL = BLOCKS.register("doorbell",
            () -> new DoorbellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5f)
                    .sound(SoundType.METAL).noCollission().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<HolidayHearthBlock> HOLIDAY_HEARTH = BLOCKS.register("holiday_hearth",
            () -> new HolidayHearthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3f, 1200f)
                    .sound(SoundType.STONE).lightLevel(s -> 12).noOcclusion().requiresCorrectToolForDrops()
                    .instrument(NoteBlockInstrument.BASEDRUM)));

    public static final Map<CarvedPumpkinVariantBlock.Face, DeferredBlock<CarvedPumpkinVariantBlock>> CARVED = new EnumMap<>(CarvedPumpkinVariantBlock.Face.class);
    public static final Map<CarvedPumpkinVariantBlock.Face, DeferredBlock<CarvedPumpkinVariantBlock>> JACK = new EnumMap<>(CarvedPumpkinVariantBlock.Face.class);

    static {
        for (CarvedPumpkinVariantBlock.Face face : CarvedPumpkinVariantBlock.Face.values()) {
            CARVED.put(face, BLOCKS.register("carved_pumpkin_" + face.id(), () -> new CarvedPumpkinVariantBlock(face, false,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1f).sound(SoundType.WOOD)
                            .pushReaction(PushReaction.DESTROY))));
            JACK.put(face, BLOCKS.register("jack_o_lantern_" + face.id(), () -> new CarvedPumpkinVariantBlock(face, true,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1f).sound(SoundType.WOOD)
                            .lightLevel(s -> 15).pushReaction(PushReaction.DESTROY))));
        }
    }

    public static final DeferredBlock<DecorBlock> TOMBSTONE = BLOCKS.register("tombstone",
            () -> new DecorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6f).sound(SoundType.STONE)
                    .noOcclusion().requiresCorrectToolForDrops(), Block.box(3, 0, 6, 13, 13, 10), false, null));

    public static final DeferredBlock<DecorBlock> HANGING_GHOST = BLOCKS.register("hanging_ghost",
            () -> new DecorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f).sound(SoundType.WOOL)
                    .noOcclusion().noCollission().pushReaction(PushReaction.DESTROY), Block.box(4, 2, 4, 12, 16, 12), true,
                    ParticleTypes.WHITE_ASH));

    private EverFestBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
