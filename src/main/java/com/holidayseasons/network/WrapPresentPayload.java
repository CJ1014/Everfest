package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.PresentBlock;
import com.holidayseasons.block.entity.PresentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: wrap the present at {@code pos}. Validated: owner, unwrapped, not empty, within reach. */
public record WrapPresentPayload(BlockPos pos, String to, String message, int color) implements CustomPacketPayload {
    public static final Type<WrapPresentPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "wrap_present"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrapPresentPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WrapPresentPayload::pos, ByteBufCodecs.stringUtf8(32), WrapPresentPayload::to,
            ByteBufCodecs.stringUtf8(120), WrapPresentPayload::message, ByteBufCodecs.INT, WrapPresentPayload::color, WrapPresentPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WrapPresentPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(p.pos.getCenter()) > 64) return;
            if (!(player.level().getBlockEntity(p.pos) instanceof PresentBlockEntity be)) return;
            if (be.info().wrapped() || be.isEmpty() || (be.owner() != null && !be.owner().equals(player.getUUID()))) {
                player.displayClientMessage(Component.translatable("holidayseasons.present.cannot_wrap"), true);
                return;
            }
            int color = p.color & 0xFFFFFF;
            be.setInfo(be.info().wrap(player.getGameProfile().getName(), player.getUUID(), p.to, p.message, color));
            player.level().setBlock(p.pos, player.level().getBlockState(p.pos).setValue(PresentBlock.WRAPPED, true), 3);
            player.level().playSound(null, p.pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1.2f);
            player.displayClientMessage(Component.translatable("holidayseasons.present.wrapped"), true);
        });
    }
}
