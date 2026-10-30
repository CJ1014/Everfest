package com.holidayseasons.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** A present as an item: place it, or (if it is for you) use it in the air to unwrap it. */
public class PresentItem extends BlockItem {
    public PresentItem(Block block, Properties properties) {
        super(block, properties.component(EverFestDataComponents.PRESENT.get(), PresentInfo.EMPTY));
    }

    public static PresentInfo info(ItemStack stack) {
        return stack.getOrDefault(EverFestDataComponents.PRESENT.get(), PresentInfo.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PresentInfo info = info(stack);
        if (!info.wrapped()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!info.canOpen(player.getGameProfile().getName(), player.getUUID())) {
            player.displayClientMessage(tagLine(info), true);
            return InteractionResultHolder.fail(stack);
        }
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        // Consume the present before handing out its contents: no window for duplication.
        ItemStack present = stack.split(1);
        for (ItemStack s : contents.nonEmptyItemsCopy()) {
            if (!player.addItem(s)) player.drop(s, false);
        }
        present.setCount(0);
        unwrapEffects((ServerLevel) level, player.position().add(0, 1, 0), info);
        return InteractionResultHolder.consume(stack);
    }

    public static Component tagLine(PresentInfo info) {
        Component to = info.to().isBlank() ? Component.translatable("holidayseasons.present.anyone") : Component.literal(info.to());
        return Component.translatable("holidayseasons.present.tag", to, info.from().isBlank() ? "?" : info.from());
    }

    public static void unwrapEffects(ServerLevel level, Vec3 at, PresentInfo info) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.6f);
        int c = info.color();
        level.sendParticles(new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.2f),
                at.x, at.y, at.z, 30, 0.4, 0.3, 0.4, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 8, 0.4, 0.3, 0.4, 0.05);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PresentInfo info = info(stack);
        if (!info.wrapped()) {
            tooltip.add(Component.translatable("holidayseasons.present.unwrapped_hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(tagLine(info).copy().withStyle(ChatFormatting.GOLD));
        if (!info.message().isBlank()) tooltip.add(Component.literal("“" + info.message() + "”").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }
}
