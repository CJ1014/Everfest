package com.holidayseasons.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.Locale;

/** A pumpkin carved with one of EverFest's faces; the lit variant is a jack-o'-lantern. */
public class CarvedPumpkinVariantBlock extends HorizontalDirectionalBlock {
    public enum Face {
        GRIN, SCARY, SILLY, CAT, GHOST;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Face next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final Face face;
    private final boolean lit;

    public CarvedPumpkinVariantBlock(Face face, boolean lit, Properties properties) {
        super(properties);
        this.face = face;
        this.lit = lit;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MapCodec.unit(this);
    }

    public Face face() {
        return face;
    }

    public boolean lit() {
        return lit;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Using a torch on a carved pumpkin lights it, turning it into the matching jack-o'-lantern. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (lit || !(stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH))) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            BlockState jack = com.holidayseasons.block.EverFestBlocks.JACK.get(face).get().defaultBlockState().setValue(FACING, state.getValue(FACING));
            level.setBlock(pos, jack, 11);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
