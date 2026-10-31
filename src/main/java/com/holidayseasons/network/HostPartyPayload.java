package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.server.EverFestServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Client -> server: host a party at this home. The server checks ownership and every party rule. */
public record HostPartyPayload(UUID homeId) implements CustomPacketPayload {
    public static final Type<HostPartyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "host_party"));
    public static final StreamCodec<ByteBuf, HostPartyPayload> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(HostPartyPayload::new, HostPartyPayload::homeId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HostPartyPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            EverFestServer server = EverFestServer.get();
            if (server == null) return;
            HomeRecord home = server.homes().get(p.homeId);
            if (home == null || (!home.owner.equals(player.getUUID()) && !player.hasPermissions(2))) return;
            if (player.blockPosition().distSqr(home.hearth) > 16 * 16) return;
            String problem = server.parties().host(player, home);
            if (problem != null) player.displayClientMessage(Component.translatable(problem), false);
        });
    }
}
