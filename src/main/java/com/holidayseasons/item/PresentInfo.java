package com.holidayseasons.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Wrapping data of a present: colour, whether it is wrapped, who it is from and for, and an optional message.
 * {@code to} empty means "for anyone".
 */
public record PresentInfo(int color, boolean wrapped, String from, Optional<UUID> fromId, String to, String message) {
    public static final PresentInfo EMPTY = new PresentInfo(0xC0392B, false, "", Optional.empty(), "", "");

    public static final Codec<PresentInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("color", 0xC0392B).forGetter(PresentInfo::color),
            Codec.BOOL.optionalFieldOf("wrapped", false).forGetter(PresentInfo::wrapped),
            Codec.STRING.optionalFieldOf("from", "").forGetter(PresentInfo::from),
            UUIDUtil.CODEC.optionalFieldOf("from_id").forGetter(PresentInfo::fromId),
            Codec.STRING.optionalFieldOf("to", "").forGetter(PresentInfo::to),
            Codec.STRING.optionalFieldOf("message", "").forGetter(PresentInfo::message)
    ).apply(i, PresentInfo::new));

    public static final StreamCodec<ByteBuf, PresentInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PresentInfo::color,
            ByteBufCodecs.BOOL, PresentInfo::wrapped,
            ByteBufCodecs.stringUtf8(64), PresentInfo::from,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), PresentInfo::fromId,
            ByteBufCodecs.stringUtf8(64), PresentInfo::to,
            ByteBufCodecs.stringUtf8(256), PresentInfo::message,
            PresentInfo::new);

    /** Whether {@code playerName} may open this present. */
    public boolean canOpen(String playerName, UUID playerId) {
        return !wrapped || to.isBlank() || to.equalsIgnoreCase(playerName);
    }

    public PresentInfo wrap(String from, UUID fromId, String to, String message, int color) {
        String t = to == null ? "" : to.strip();
        String m = message == null ? "" : message.strip();
        return new PresentInfo(color, true, from, Optional.of(fromId), t.length() > 32 ? t.substring(0, 32) : t, m.length() > 120 ? m.substring(0, 120) : m);
    }
}
