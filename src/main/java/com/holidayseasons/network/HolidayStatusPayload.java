package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: holiday occurrences around today, in the scheduling calendar identified by {@code signature}. */
public record HolidayStatusPayload(String signature, List<HolidayStatus> statuses) implements CustomPacketPayload {
    public static final Type<HolidayStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "holiday_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HolidayStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.signature, 512);
                buf.writeVarInt(p.statuses.size());
                for (HolidayStatus s : p.statuses) {
                    buf.writeUtf(s.id(), 256);
                    buf.writeInt(s.color());
                    NetCodecs.writeDate(buf, s.prepStart());
                    NetCodecs.writeDate(buf, s.start());
                    NetCodecs.writeDate(buf, s.end());
                    buf.writeEnum(s.phase());
                    buf.writeVarLong(s.daysUntilStart());
                    buf.writeUtf(s.skippedReason(), 64);
                }
            },
            buf -> {
                String sig = buf.readUtf(512);
                int n = Math.min(buf.readVarInt(), 512);
                List<HolidayStatus> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(new HolidayStatus(buf.readUtf(256), buf.readInt(), NetCodecs.readDate(buf), NetCodecs.readDate(buf),
                            NetCodecs.readDate(buf), buf.readEnum(HolidayPhase.class), buf.readVarLong(), buf.readUtf(64)));
                }
                return new HolidayStatusPayload(sig, list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
