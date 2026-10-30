package com.holidayseasons.village;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** All villages EverFest has seen ({@code holidayseasons_villages.dat}). */
public final class VillageRegistryData extends SavedData {
    public static final String FILE_ID = "holidayseasons_villages";
    public static final Factory<VillageRegistryData> FACTORY = new Factory<>(VillageRegistryData::new, VillageRegistryData::load, null);

    private final Map<String, VillageRecord> villages = new HashMap<>();

    public static VillageRegistryData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageRegistryData d = new VillageRegistryData();
        ListTag list = tag.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            VillageRecord r = VillageRecord.load(list.getCompound(i));
            d.villages.put(r.key, r);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (VillageRecord r : villages.values()) list.add(r.save());
        tag.put("villages", list);
        return tag;
    }

    public VillageRecord getOrCreate(String key, net.minecraft.core.BlockPos centre) {
        VillageRecord r = villages.get(key);
        if (r == null) {
            r = new VillageRecord(key, centre);
            villages.put(key, r);
            setDirty();
        }
        return r;
    }

    public VillageRecord get(String key) {
        return villages.get(key);
    }

    public Collection<VillageRecord> all() {
        return villages.values();
    }

    /** Forget per-occurrence progress that no longer matters. */
    public void retainOccurrences(Set<String> liveOrRecent) {
        boolean changed = false;
        for (VillageRecord r : villages.values()) {
            changed |= r.celebrations.keySet().removeIf(k -> !liveOrRecent.contains(k));
        }
        if (changed) setDirty();
    }
}
