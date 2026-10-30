package com.holidayseasons.block;

import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A doorbell. Rings with a chime, emits a short redstone pulse (like a button) and tells the home's owner someone is
 * at the door. Village trick-or-treaters ring it too.
 */
public class DoorbellBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<DoorbellBlock> CODEC = simpleCodec(DoorbellBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PRESS_TICKS = 16;

    public DoorbellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, FACE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        float depth = state.getValue(POWERED) ? 1 : 2;
        return switch (state.getValue(FACE)) {
            case FLOOR -> Block.box(5, 0, 5, 11, depth, 11);
            case CEILING -> Block.box(5, 16 - depth, 5, 11, 16, 11);
            case WALL -> switch (facing) {
                case SOUTH -> Block.box(5, 5, 0, 11, 11, depth);
                case WEST -> Block.box(16 - depth, 5, 5, 16, 11, 11);
                case EAST -> Block.box(0, 5, 5, depth, 11, 11);
                default -> Block.box(5, 5, 16 - depth, 11, 11, 16);
            };
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(POWERED)) return InteractionResult.CONSUME;
        if (!level.isClientSide) ring((ServerLevel) level, pos, state, player instanceof ServerPlayer sp ? sp : null, null);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Rings the bell. {@code visitorName} is used when a villager rings. */
    public static void ring(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer ringer, @Nullable Component visitorName) {
        if (!(state.getBlock() instanceof DoorbellBlock) || state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, true), 3);
        updateNeighbours(state, level, pos);
        level.scheduleTick(pos, state.getBlock(), PRESS_TICKS);
        level.playSound(null, pos, EverFestSounds.DOORBELL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        EverFestServer server = EverFestServer.get();
        if (server == null) return;
        HomeRecord home = server.homes().near(level.dimension(), pos, 12);
        if (home == null || (ringer != null && ringer.getUUID().equals(home.owner))) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(home.owner);
        if (owner != null && owner.level() == level && owner.blockPosition().distSqr(pos) < 96 * 96) {
            Component who = ringer != null ? ringer.getDisplayName() : visitorName != null ? visitorName : Component.translatable("holidayseasons.doorbell.someone");
            owner.displayClientMessage(Component.translatable("holidayseasons.doorbell.rang", who, home.name()), true);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
            updateNeighbours(state, level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(POWERED)) updateNeighbours(state, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    private static void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
    }
}
