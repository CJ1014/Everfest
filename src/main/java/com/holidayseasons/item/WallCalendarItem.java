package com.holidayseasons.item;

import com.holidayseasons.network.OpenScreenPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

/** Places a wall calendar on a wall; used in the air it opens the calendar like a pocket planner. */
public class WallCalendarItem extends BlockItem {
    public WallCalendarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new OpenScreenPayload(OpenScreenPayload.Screen.CALENDAR));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
