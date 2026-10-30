package com.holidayseasons.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent, abstract state of one village. Survives while the village is unloaded so celebrations can be
 * reconciled when players come back, without ever loading chunks.
 */
public final class VillageRecord {
    public final String key;
    BlockPos centre;
    VillageStyle style = VillageStyle.PLAINS;
    /** Scheduling-calendar day the village was last seen loaded. */
    long lastSeenDay = Long.MIN_VALUE;
    final Map<String, Celebration> celebrations = new HashMap<>();

    /** Per-occurrence progress. */
    public static final class Celebration {
        /** Decorations ever placed for this occurrence (including ones already removed). */
        int placed;
        boolean centrepiece;
        /** Day on which unloaded-time catch-up was last applied. */
        long catchUpDay = Long.MIN_VALUE;
        int villagersTakingPart;
        /** Trick-or-treat visits per door this occurrence (so the same door is not knocked on endlessly). */
        final Map<String, Integer> visits = new HashMap<>();

        public int visits(String doorKey) {
            return visits.getOrDefault(doorKey, 0);
        }

        public void recordVisit(String doorKey) {
            visits.merge(doorKey, 1, Integer::sum);
        }

        public int placed() {
            return placed;
        }

        public void recordPlaced(boolean isCentrepiece) {
            placed++;
            if (isCentrepiece) centrepiece = true;
        }

        public boolean centrepiece() {
            return centrepiece;
        }
    }

    public VillageRecord(String key, BlockPos centre) {
        this.key = key;
        this.centre = centre;
    }

    public BlockPos centre() {
        return centre;
    }

    public VillageStyle style() {
        return style;
    }

    public long lastSeenDay() {
        return lastSeenDay;
    }

    public void setLastSeenDay(long day) {
        this.lastSeenDay = day;
    }

    public void setStyle(VillageStyle style) {
        this.style = style;
    }

    public void setCentre(BlockPos centre) {
        this.centre = centre;
    }

    public Celebration celebration(String occurrenceKey) {
        return celebrations.computeIfAbsent(occurrenceKey, k -> new Celebration());
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("key", key);
        t.put("centre", NbtUtils.writeBlockPos(centre));
        t.putString("style", style.id());
        t.putLong("lastSeen", lastSeenDay);
        CompoundTag cs = new CompoundTag();
        celebrations.forEach((occ, c) -> {
            CompoundTag ct = new CompoundTag();
            ct.putInt("placed", c.placed);
            ct.putBoolean("centrepiece", c.centrepiece);
            ct.putLong("catchUp", c.catchUpDay);
            CompoundTag v = new CompoundTag();
            c.visits.forEach(v::putInt);
            ct.put("visits", v);
            cs.put(occ, ct);
        });
        t.put("celebrations", cs);
        return t;
    }

    static VillageRecord load(CompoundTag t) {
        VillageRecord r = new VillageRecord(t.getString("key"), NbtUtils.readBlockPos(t, "centre").orElse(BlockPos.ZERO));
        r.style = VillageStyle.byId(t.getString("style"));
        r.lastSeenDay = t.getLong("lastSeen");
        CompoundTag cs = t.getCompound("celebrations");
        for (String occ : cs.getAllKeys()) {
            CompoundTag ct = cs.getCompound(occ);
            Celebration c = new Celebration();
            c.placed = ct.getInt("placed");
            c.centrepiece = ct.getBoolean("centrepiece");
            c.catchUpDay = ct.getLong("catchUp");
            CompoundTag v = ct.getCompound("visits");
            for (String k : v.getAllKeys()) c.visits.put(k, v.getInt(k));
            r.celebrations.put(occ, c);
        }
        return r;
    }
}
