package com.holidayseasons.item;

import com.holidayseasons.block.CarvedPumpkinVariantBlock;
import com.holidayseasons.block.EverFestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Pumpkin carving. Use on a pumpkin to carve a grin (dropping seeds, like shears); use on a carved pumpkin again to
 * re-carve the next face. Lighting it is done by using a torch on it.
 */
public class CarvingKnifeItem extends Item {
    public CarvingKnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        Direction facing = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace()
                : (player != null ? player.getDirection().getOpposite() : Direction.NORTH);
        BlockState carved;
        if (state.is(Blocks.PUMPKIN)) {
            carved = EverFestBlocks.CARVED.get(CarvedPumpkinVariantBlock.Face.GRIN).get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            if (!level.isClientSide) {
                ItemEntity seeds = new ItemEntity(level, pos.getX() + 0.5 + facing.getStepX() * 0.65, pos.getY() + 0.1,
                        pos.getZ() + 0.5 + facing.getStepZ() * 0.65, new ItemStack(Items.PUMPKIN_SEEDS, 2));
                level.addFreshEntity(seeds);
            }
        } else if (state.getBlock() instanceof CarvedPumpkinVariantBlock variant) {
            CarvedPumpkinVariantBlock.Face next = variant.face().next();
            carved = (variant.lit() ? EverFestBlocks.JACK : EverFestBlocks.CARVED).get(next).get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING));
        } else {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, carved, 11);
            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1f, 1f);
            ((ServerLevel) level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PUMPKIN.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
            ItemStack stack = ctx.getItemInHand();
            if (player != null) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
