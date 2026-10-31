package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.config.AdminConfig;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Operator client -> server: changed settings. The server re-checks the sender's permission and every value. */
public record SetConfigPayload(Map<String, String> changes) implements CustomPacketPayload {
    public static final Type<SetConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "set_config"));
    public static final StreamCodec<FriendlyByteBuf, SetConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.changes.size());
                p.changes.forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeUtf(v, 4096);
                });
            },
            buf -> {
                int n = Math.min(buf.readVarInt(), 512);
                Map<String, String> m = new LinkedHashMap<>();
                for (int i = 0; i < n; i++) m.put(buf.readUtf(), buf.readUtf(4096));
                return new SetConfigPayload(m);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetConfigPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) {
                EverFest.LOGGER.warn("EverFest: {} tried to change server settings without permission", player.getGameProfile().getName());
                return;
            }
            String problem = AdminConfig.apply(p.changes);
            if (problem != null) {
                player.sendSystemMessage(Component.translatable("holidayseasons.admin.rejected", problem).withStyle(ChatFormatting.RED));
                return;
            }
            EverFestServer s = EverFestServer.get();
            if (s != null) {
                s.calendar().reloadSettings();
                s.holidays().reloadDefinitions();
            }
            player.sendSystemMessage(Component.translatable("holidayseasons.admin.saved", p.changes.size()).withStyle(ChatFormatting.GREEN));
        });
    }
}
