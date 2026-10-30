package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.holiday.HolidayPhase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: a holiday just started preparing or celebrating (shown as a toast). */
public record HolidayNoticePayload(String holidayId, HolidayPhase phase, int color) implements CustomPacketPayload {
    public static final Type<HolidayNoticePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "holiday_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HolidayNoticePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.holidayId, 256);
                buf.writeEnum(p.phase);
                buf.writeInt(p.color);
            },
            buf -> new HolidayNoticePayload(buf.readUtf(256), buf.readEnum(HolidayPhase.class), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
