package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: open the wrapping screen for a present. */
public record OpenWrapPayload(BlockPos pos, int color) implements CustomPacketPayload {
    public static final Type<OpenWrapPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "open_wrap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWrapPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenWrapPayload::pos, ByteBufCodecs.INT, OpenWrapPayload::color, OpenWrapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
