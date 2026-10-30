package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Client -> server: new hearth settings. The server checks that the sender owns the home (or is an operator). */
public record UpdateHearthPayload(UUID homeId, String name, boolean acceptVisitors, boolean allowVillagerDecorations,
                                  HomeRecord.CandyPolicy policy) implements CustomPacketPayload {
    public static final Type<UpdateHearthPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "update_hearth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateHearthPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.homeId);
                buf.writeUtf(p.name, 64);
                buf.writeBoolean(p.acceptVisitors);
                buf.writeBoolean(p.allowVillagerDecorations);
                buf.writeEnum(p.policy);
            },
            buf -> new UpdateHearthPayload(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(64), buf.readBoolean(), buf.readBoolean(),
                    buf.readEnum(HomeRecord.CandyPolicy.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateHearthPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            EverFestServer server = EverFestServer.get();
            if (server == null) return;
            HomeRecord home = server.homes().get(p.homeId);
            if (home == null) return;
            if (!home.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
                player.displayClientMessage(Component.translatable("holidayseasons.home.not_owner"), true);
                return;
            }
            // The player must actually be near the hearth they are editing.
            if (player.level().dimension() != home.dimension || player.blockPosition().distSqr(home.hearth) > 16 * 16) return;
            home.applySettings(p.name, p.acceptVisitors, p.allowVillagerDecorations, p.policy);
            server.homes().setDirty();
            player.displayClientMessage(Component.translatable("holidayseasons.home.saved", home.name()), true);
        });
    }
}
