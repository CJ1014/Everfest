package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: open an EverFest screen. Keeps screen opening server-driven so blocks work on dedicated servers. */
public record OpenScreenPayload(Screen screen) implements CustomPacketPayload {
    public enum Screen { CALENDAR, COLLECTION_BOOK, ADMIN }

    public static final Type<OpenScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "open_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Screen.values()[i], Screen::ordinal), OpenScreenPayload::screen,
            OpenScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
