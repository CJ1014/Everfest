package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: the player's Holiday Collection Book; the client opens it. */
public record CollectionPayload(CompoundTag book) implements CustomPacketPayload {
    public static final Type<CollectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "collection"));
    public static final StreamCodec<FriendlyByteBuf, CollectionPayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(CollectionPayload::new, CollectionPayload::book).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
