package com.holidayseasons.network;

import com.holidayseasons.EverFest;
import com.holidayseasons.config.AdminConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server -> operator client: every server setting, to open the admin config screen. */
public record AdminConfigPayload(List<AdminConfig.Entry> entries) implements CustomPacketPayload {
    public static final Type<AdminConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EverFest.MOD_ID, "admin_config"));
    public static final StreamCodec<FriendlyByteBuf, AdminConfigPayload> STREAM_CODEC = StreamCodec.of(AdminConfigPayload::write, AdminConfigPayload::read);

    private static void write(FriendlyByteBuf buf, AdminConfigPayload p) {
        buf.writeVarInt(p.entries.size());
        for (AdminConfig.Entry e : p.entries) {
            buf.writeUtf(e.path());
            buf.writeEnum(e.type());
            buf.writeUtf(e.value(), 4096);
            buf.writeUtf(e.min());
            buf.writeUtf(e.max());
            buf.writeVarInt(e.options().size());
            e.options().forEach(o -> buf.writeUtf(o));
            buf.writeUtf(e.comment(), 2048);
            buf.writeUtf(e.translationKey());
        }
    }

    private static AdminConfigPayload read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<AdminConfig.Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String path = buf.readUtf();
            AdminConfig.Type type = buf.readEnum(AdminConfig.Type.class);
            String value = buf.readUtf(4096);
            String min = buf.readUtf();
            String max = buf.readUtf();
            int no = buf.readVarInt();
            List<String> options = new ArrayList<>(no);
            for (int k = 0; k < no; k++) options.add(buf.readUtf());
            list.add(new AdminConfig.Entry(path, type, value, min, max, options, buf.readUtf(2048), buf.readUtf()));
        }
        return new AdminConfigPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
