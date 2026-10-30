package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.CalendarSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: the authoritative calendar state. */
public record CalendarSyncPayload(CalendarSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<CalendarSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "calendar_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CalendarSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> NetCodecs.writeSnapshot(buf, p.snapshot),
            buf -> new CalendarSyncPayload(NetCodecs.readSnapshot(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
