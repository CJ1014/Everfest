package com.holidayseasons.holiday;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted holiday occurrence records ({@code holidayseasons_holidays.dat} in the overworld data folder). */
public final class HolidayStateData extends SavedData {
    public static final String FILE_ID = "holidayseasons_holidays";
    public static final Factory<HolidayStateData> FACTORY = new Factory<>(HolidayStateData::new, HolidayStateData::load, null);

    final Map<String, OccurrenceRecord> records = new LinkedHashMap<>();

    public static HolidayStateData load(CompoundTag tag, HolderLookup.Provider registries) {
        HolidayStateData d = new HolidayStateData();
        ListTag list = tag.getList("occurrences", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            OccurrenceRecord r = OccurrenceRecord.load(list.getCompound(i));
            d.records.put(r.key(), r);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (OccurrenceRecord r : records.values()) list.add(r.save());
        tag.put("occurrences", list);
        return tag;
    }

    public Map<String, OccurrenceRecord> records() {
        return records;
    }
}
