package com.holidayseasons.block;

import com.holidayseasons.block.entity.HearthBlockEntity;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.home.HomeRegistry;
import com.holidayseasons.network.OpenHearthPayload;
import com.holidayseasons.server.EverFestServer;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A cosy little hearth that registers the surrounding house as the player's home: villagers trick-or-treat and visit
 * here, and the owner configures visitors through its screen. Only the owner (or an operator) can break it.
 */
public class HolidayHearthBlock extends BaseEntityBlock {
    public static final MapCodec<HolidayHearthBlock> CODEC = simpleCodec(HolidayHearthBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 12, 14), Block.box(1, 12, 1, 15, 14, 15));

    public HolidayHearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HearthBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return null;
        if (!ctx.getLevel().isClientSide) {
            EverFestServer server = EverFestServer.get();
            if (server != null) {
                String problem = server.homes().validate(player.getUUID(), ctx.getLevel().dimension(), ctx.getClickedPos());
                if (problem != null) {
                    player.displayClientMessage(Component.translatable(problem, HomeRegistry.maxHomesPerPlayer(), HomeRegistry.minDistanceBetweenOwners()), true);
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || !(placer instanceof ServerPlayer player)) return;
        EverFestServer server = EverFestServer.get();
        if (server == null || !(level.getBlockEntity(pos) instanceof HearthBlockEntity be)) return;
        HomeRecord home = server.homes().register((ServerLevel) level, player.getUUID(), player.getGameProfile().getName(), pos);
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            home.applySettings(stack.getHoverName().getString(), true, false, HomeRecord.CandyPolicy.EVERYONE);
        }
        be.setHomeId(home.id);
        boolean hasDoor = !home.entrance().equals(pos);
        player.displayClientMessage(Component.translatable(hasDoor ? "holidayseasons.home.registered" : "holidayseasons.home.registered_no_door",
                home.name(), home.entrance().toShortString()), false);
        com.holidayseasons.reputation.EverFestAdvancements.award(player, com.holidayseasons.reputation.EverFestAdvancements.HOME_SWEET_HOME);
    }

    /**
     * A hearth nobody owns (for example in a seasonal cabin found in the wild): sneak-use claims the house, with the
     * same limits as placing a new hearth.
     */
    private static void claimAbandoned(ServerLevel level, BlockPos pos, ServerPlayer player, EverFestServer server) {
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("holidayseasons.home.unregistered"), true);
            return;
        }
        String problem = server.homes().validate(player.getUUID(), level.dimension(), pos);
        if (problem != null) {
            player.displayClientMessage(Component.translatable(problem, HomeRegistry.maxHomesPerPlayer(), HomeRegistry.minDistanceBetweenOwners()), true);
            return;
        }
        HomeRecord home = server.homes().register(level, player.getUUID(), player.getGameProfile().getName(), pos);
        if (level.getBlockEntity(pos) instanceof HearthBlockEntity be) be.setHomeId(home.id);
        boolean hasDoor = !home.entrance().equals(pos);
        player.displayClientMessage(Component.translatable(hasDoor ? "holidayseasons.home.registered" : "holidayseasons.home.registered_no_door",
                home.name(), home.entrance().toShortString()), false);
        com.holidayseasons.reputation.EverFestAdvancements.award(player, com.holidayseasons.reputation.EverFestAdvancements.HOME_SWEET_HOME);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        EverFestServer server = EverFestServer.get();
        if (server == null || !(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;
        HomeRecord home = server.homes().atHearth(level.dimension(), pos);
        if (home == null) {
            claimAbandoned((ServerLevel) level, pos, sp, server);
            return InteractionResult.CONSUME;
        }
        if (home.owner.equals(player.getUUID()) || player.hasPermissions(2)) {
            server.homes().refreshEntrance((ServerLevel) level, home);
            PacketDistributor.sendToPlayer(sp, OpenHearthPayload.of(home));
        } else {
            player.displayClientMessage(Component.translatable("holidayseasons.home.belongs_to", home.name(), home.ownerName()), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            EverFestServer server = EverFestServer.get();
            if (server != null) {
                HomeRecord home = server.homes().atHearth(level.dimension(), pos);
                if (home != null) server.homes().unregister(home.id);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.85, z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + random.nextGaussian() * 0.12, y, z + random.nextGaussian() * 0.12, 0, 0.01, 0);
        if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0.03, 0);
        if (random.nextInt(40) == 0) level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1f, false);
    }
}
