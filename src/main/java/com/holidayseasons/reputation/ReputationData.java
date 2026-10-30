package com.holidayseasons.reputation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player, per-village holiday reputation ({@code holidayseasons_reputation.dat}). */
public final class ReputationData extends SavedData {
    public static final String FILE_ID = "holidayseasons_reputation";
    public static final Factory<ReputationData> FACTORY = new Factory<>(ReputationData::new, ReputationData::load, null);

    private final Map<UUID, Map<String, Integer>> points = new HashMap<>();
    /** player|village|action|day -> count, for diminishing returns. Not persisted (day-scoped). */
    private final Map<String, Integer> today = new HashMap<>();
    private long todayDay = Long.MIN_VALUE;

    public static ReputationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReputationData d = new ReputationData();
        for (String player : tag.getAllKeys()) {
            CompoundTag villages = tag.getCompound(player);
            Map<String, Integer> m = new HashMap<>();
            for (String v : villages.getAllKeys()) m.put(v, villages.getInt(v));
            d.points.put(UUID.fromString(player), m);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        points.forEach((player, villages) -> {
            CompoundTag v = new CompoundTag();
            villages.forEach(v::putInt);
            tag.put(player.toString(), v);
        });
        return tag;
    }

    public int get(UUID player, String village) {
        return points.getOrDefault(player, Map.of()).getOrDefault(village, 0);
    }

    public int add(UUID player, String village, ReputationMath.Action action, long day) {
        if (day != todayDay) {
            today.clear();
            todayDay = day;
        }
        String key = player + "|" + village + "|" + action;
        int count = today.getOrDefault(key, 0);
        int updated = ReputationMath.apply(get(player, village), action, count);
        today.put(key, count + 1);
        points.computeIfAbsent(player, k -> new HashMap<>()).put(village, updated);
        setDirty();
        return updated;
    }

    /** Operator override. */
    public void set(UUID player, String village, int value) {
        points.computeIfAbsent(player, k -> new HashMap<>()).put(village, Math.max(ReputationMath.MIN, Math.min(ReputationMath.MAX, value)));
        setDirty();
    }

    public Map<String, Integer> of(UUID player) {
        return Map.copyOf(points.getOrDefault(player, Map.of()));
    }
}
