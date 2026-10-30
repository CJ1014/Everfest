package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.home.HomeRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Server -> client: open the Holiday Hearth settings screen. */
public record OpenHearthPayload(UUID homeId, String name, String ownerName, BlockPos hearth, BlockPos entrance, boolean acceptVisitors,
                                boolean allowVillagerDecorations, HomeRecord.CandyPolicy policy, int visits) implements CustomPacketPayload {
    public static final Type<OpenHearthPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "open_hearth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHearthPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.homeId);
                buf.writeUtf(p.name, 64);
                buf.writeUtf(p.ownerName, 64);
                buf.writeBlockPos(p.hearth);
                buf.writeBlockPos(p.entrance);
                buf.writeBoolean(p.acceptVisitors);
                buf.writeBoolean(p.allowVillagerDecorations);
                buf.writeEnum(p.policy);
                buf.writeVarInt(p.visits);
            },
            buf -> new OpenHearthPayload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(64), buf.readUtf(64), buf.readBlockPos(),
                    buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readEnum(HomeRecord.CandyPolicy.class), buf.readVarInt()));

    public static OpenHearthPayload of(HomeRecord h) {
        return new OpenHearthPayload(h.id, h.name(), h.ownerName(), h.hearth, h.entrance(), h.acceptVisitors(),
                h.allowVillagerDecorations(), h.candyPolicy(), h.visitsThisYear());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
