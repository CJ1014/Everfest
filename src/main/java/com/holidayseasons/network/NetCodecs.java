package com.holidayseasons.network;

import com.holidayseasons.calendar.CalendarDate;
import com.holidayseasons.calendar.CalendarMode;
import com.holidayseasons.calendar.CalendarSnapshot;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.calendar.SubSeason;
import net.minecraft.network.FriendlyByteBuf;

/** Hand-written wire formats for EverFest's pure data types. */
public final class NetCodecs {
    private NetCodecs() {
    }

    public static void writeDate(FriendlyByteBuf buf, CalendarDate d) {
        buf.writeVarInt(d.year());
        buf.writeByte(d.month());
        buf.writeVarInt(d.day());
    }

    public static CalendarDate readDate(FriendlyByteBuf buf) {
        int year = buf.readVarInt();
        int month = buf.readByte();
        int day = buf.readVarInt();
        return new CalendarDate(year, month, day);
    }

    public static void writeSnapshot(FriendlyByteBuf buf, CalendarSnapshot s) {
        buf.writeEnum(s.mode());
        buf.writeUtf(s.systemSignature(), 512);
        writeDate(buf, s.date());
        buf.writeVarLong(s.epochDay());
        buf.writeVarInt(s.timeOfDay());
        buf.writeByte(s.dayOfWeek());
        buf.writeEnum(s.season());
        buf.writeEnum(s.subSeason());
        buf.writeBoolean(s.seasonsFromSereneSeasons());
        for (int len : s.monthLengths()) buf.writeVarInt(len);
    }

    public static CalendarSnapshot readSnapshot(FriendlyByteBuf buf) {
        CalendarMode mode = buf.readEnum(CalendarMode.class);
        String sig = buf.readUtf(512);
        CalendarDate date = readDate(buf);
        long epochDay = buf.readVarLong();
        int tod = buf.readVarInt();
        int dow = buf.readByte();
        Season season = buf.readEnum(Season.class);
        SubSeason sub = buf.readEnum(SubSeason.class);
        boolean ss = buf.readBoolean();
        int[] lengths = new int[12];
        for (int i = 0; i < 12; i++) lengths[i] = buf.readVarInt();
        return new CalendarSnapshot(mode, sig, date, epochDay, tod, dow, season, sub, ss, lengths);
    }
}
