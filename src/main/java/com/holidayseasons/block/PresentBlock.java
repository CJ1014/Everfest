package com.holidayseasons.block;

import com.holidayseasons.block.entity.PresentBlockEntity;
import com.holidayseasons.item.EverFestDataComponents;
import com.holidayseasons.item.PresentInfo;
import com.holidayseasons.item.PresentItem;
import com.holidayseasons.network.OpenWrapPayload;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A gift box. Unwrapped, its owner fills it (right-click) and wraps it (sneak + right-click), naming a recipient and a
 * message. Wrapped, the recipient (or anyone, if no recipient was named) opens it with a right-click; everyone else
 * just reads the tag. Breaking it keeps the contents in the item.
 */
public class PresentBlock extends BaseEntityBlock {
    public static final MapCodec<PresentBlock> CODEC = simpleCodec(p -> new PresentBlock(p, 10));
    public static final BooleanProperty WRAPPED = BooleanProperty.create("wrapped");
    private final VoxelShape shape;

    public PresentBlock(Properties properties, int size) {
        super(properties);
        int half = size / 2;
        this.shape = Block.box(8 - half, 0, 8 - half, 8 + half, size, 8 + half);
        registerDefaultState(stateDefinition.any().setValue(WRAPPED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WRAPPED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PresentBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof PresentBlockEntity be) {
            if (placer instanceof Player p && !be.info().wrapped()) be.setOwner(p.getUUID());
            if (be.info().wrapped() != state.getValue(WRAPPED)) level.setBlock(pos, state.setValue(WRAPPED, be.info().wrapped()), 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PresentBlockEntity be)) return InteractionResult.FAIL;
        PresentInfo info = be.info();
        if (!info.wrapped()) {
            if (be.owner() != null && !be.owner().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("holidayseasons.present.not_yours"), true);
                return InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown()) {
                if (be.isEmpty()) {
                    player.displayClientMessage(Component.translatable("holidayseasons.present.empty"), true);
                } else if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new OpenWrapPayload(pos, info.color()));
                }
            } else {
                player.openMenu(be);
            }
            return InteractionResult.CONSUME;
        }
        if (!info.canOpen(player.getGameProfile().getName(), player.getUUID())) {
            player.displayClientMessage(PresentItem.tagLine(info), true);
            return InteractionResult.CONSUME;
        }
        // Unwrap: take the contents out first, then remove the block, so nothing can be duplicated.
        for (ItemStack s : be.takeAll()) {
            if (!s.isEmpty() && !player.addItem(s)) player.drop(s, false);
        }
        be.setInfo(PresentInfo.EMPTY);
        level.removeBlock(pos, false);
        PresentItem.unwrapEffects((ServerLevel) level, pos.getCenter(), info);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Contents travel with the dropped item (loot table copies the container component), like a shulker box.
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // In creative, vanilla would not drop anything: keep the gift by dropping it explicitly, as shulker boxes do.
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof PresentBlockEntity be && !be.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(be.collectComponents());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            be.takeAll();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    public static ItemStack withInfo(ItemStack stack, PresentInfo info) {
        stack.set(EverFestDataComponents.PRESENT.get(), info);
        return stack;
    }
}
