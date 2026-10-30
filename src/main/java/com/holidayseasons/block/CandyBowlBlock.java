package com.holidayseasons.block;

import com.holidayseasons.block.entity.CandyBowlBlockEntity;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.Containers;
import org.jetbrains.annotations.Nullable;

/**
 * A bowl of candy for trick-or-treaters. Right-click with candy to fill it; right-click empty-handed to take one
 * (owners take freely, visitors are limited per day). Village children are served automatically.
 */
public class CandyBowlBlock extends BaseEntityBlock {
    public static final MapCodec<CandyBowlBlock> CODEC = simpleCodec(CandyBowlBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 5, 13);

    public CandyBowlBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
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
        return new CandyBowlBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player p && level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl) {
            bowl.setOwner(p.getUUID());
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(EverFestItems.CANDY_TAG)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl)) return ItemInteractionResult.FAIL;
        int before = stack.getCount();
        ItemStack rest = bowl.add(stack.copy());
        int added = before - rest.getCount();
        if (added > 0) {
            if (!player.getAbilities().instabuild) stack.shrink(added);
            level.playSound(null, pos, EverFestSounds.CANDY.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0);
        } else {
            player.displayClientMessage(Component.translatable("holidayseasons.candy_bowl.full"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl)) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("holidayseasons.candy_bowl.contents", bowl.candyCount(), bowl.dispensedTotal()), true);
            return InteractionResult.CONSUME;
        }
        boolean isOwner = player.getUUID().equals(bowl.owner());
        EverFestServer server = EverFestServer.get();
        long day = server == null ? 0 : server.calendar().snapshot().epochDay();
        if (!isOwner && server != null) {
            HomeRecord home = server.homes().near(level.dimension(), pos, 10);
            if (home != null && home.candyPolicy() == HomeRecord.CandyPolicy.OWNER_ONLY && !home.owner.equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("holidayseasons.candy_bowl.owner_only", home.name()), true);
                return InteractionResult.CONSUME;
            }
            int limit = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.candyPerVisitorPerDay.get() : 1;
            if (bowl.takenOn(player.getUUID(), day) >= limit) {
                player.displayClientMessage(Component.translatable("holidayseasons.candy_bowl.limit"), true);
                return InteractionResult.CONSUME;
            }
        }
        ItemStack candy = bowl.takeOne();
        if (candy.isEmpty()) {
            emptyFeedback((ServerLevel) level, pos);
            player.displayClientMessage(Component.translatable("holidayseasons.candy_bowl.empty"), true);
            return InteractionResult.CONSUME;
        }
        if (!isOwner) bowl.recordTaken(player.getUUID(), day);
        if (!player.addItem(candy)) player.drop(candy, false);
        level.playSound(null, pos, EverFestSounds.CANDY.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.CONSUME;
    }

    /** Serves a village trick-or-treater: one candy, or empty. Credits the bowl owner's reputation with the village. */
    public static ItemStack serveVillager(ServerLevel level, BlockPos pos, String villageKey) {
        if (!(level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl)) return ItemStack.EMPTY;
        ItemStack candy = bowl.takeOne();
        if (candy.isEmpty()) {
            emptyFeedback(level, pos);
            return ItemStack.EMPTY;
        }
        level.playSound(null, pos, EverFestSounds.CANDY.get(), SoundSource.BLOCKS, 0.8f, 1.3f);
        EverFestServer server = EverFestServer.get();
        if (server != null && bowl.owner() != null) {
            server.reputation().add(bowl.owner(), villageKey, ReputationMath.Action.GAVE_CANDY, server.calendar().snapshot().epochDay());
        }
        return candy;
    }

    private static void emptyFeedback(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, EverFestSounds.CANDY_BOWL_EMPTY.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 5, 0.15, 0.05, 0.15, 0.01);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl) {
            for (int i = 0; i < CandyBowlBlockEntity.SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bowl.inventory().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(FILL) * 5;
    }
}
