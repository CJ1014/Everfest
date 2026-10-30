package com.holidayseasons.decoration;

import com.holidayseasons.EverFest;
import com.mojang.math.Transformation;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records every temporary world change EverFest makes so it can be undone safely.
 *
 * <p>Safety rules:</p>
 * <ul>
 *   <li>Decorations are block-display entities: they never replace a block, have no collision and drop nothing.</li>
 *   <li>The only block change is an invisible {@code minecraft:light} block, placed only into air. It is removed only
 *   if the block is still exactly that light block; if a player has built there since, the entry is dropped and the
 *   player's block is left alone.</li>
 *   <li>Each display entity carries its own ledger id and occurrence key; when an entity loads and the ledger no longer
 *   knows it (e.g. the ledger was lost in a crash) or it has expired, it is removed before it joins the world.</li>
 *   <li>Nothing is done in unloaded chunks; due entries wait until their chunk loads. Chunks are never force-loaded.</li>
 * </ul>
 */
public final class DecorationLedger extends SavedData {
    public static final String FILE_ID = "holidayseasons_decorations";
    public static final String ENTITY_TAG = "holidayseasons_decoration";
    public static final Factory<DecorationLedger> FACTORY = new Factory<>(DecorationLedger::new, DecorationLedger::load, null);

    public enum Kind { DISPLAY, LIGHT_BLOCK }

    public static final class Entry {
        final long id;
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final Kind kind;
        @Nullable
        final UUID entity;
        @Nullable
        final BlockState placed;
        final String occurrenceKey;
        final String villageKey;
        final String pieceId;
        /** Scheduling-calendar epoch day from which the entry may be removed; Long.MAX_VALUE = not scheduled yet. */
        long expiresDay = Long.MAX_VALUE;
        /** Remove as soon as possible (holiday aborted / finished). */
        boolean expired;

        Entry(long id, ResourceKey<Level> dimension, BlockPos pos, Kind kind, @Nullable UUID entity, @Nullable BlockState placed,
              String occurrenceKey, String villageKey, String pieceId) {
            this.id = id;
            this.dimension = dimension;
            this.pos = pos;
            this.kind = kind;
            this.entity = entity;
            this.placed = placed;
            this.occurrenceKey = occurrenceKey;
            this.villageKey = villageKey;
            this.pieceId = pieceId;
        }

        public BlockPos pos() {
            return pos;
        }

        public String pieceId() {
            return pieceId;
        }

        public String occurrenceKey() {
            return occurrenceKey;
        }

        public String villageKey() {
            return villageKey;
        }

        public Kind kind() {
            return kind;
        }

        public boolean isDue(long today) {
            return expired || today >= expiresDay;
        }
    }

    private final Map<Long, Entry> entries = new LinkedHashMap<>();
    private long nextId = 1;
    private Iterator<Entry> cursor;

    public static DecorationLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        DecorationLedger l = new DecorationLedger();
        l.nextId = Math.max(1, tag.getLong("nextId"));
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim")));
            Kind kind = t.getString("kind").equals("LIGHT_BLOCK") ? Kind.LIGHT_BLOCK : Kind.DISPLAY;
            BlockState placed = t.contains("placed") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), t.getCompound("placed")) : null;
            Entry e = new Entry(t.getLong("id"), dim, NbtUtils.readBlockPos(t, "pos").orElse(BlockPos.ZERO), kind,
                    t.hasUUID("entity") ? t.getUUID("entity") : null, placed, t.getString("occ"), t.getString("village"), t.getString("piece"));
            e.expiresDay = t.contains("expires") ? t.getLong("expires") : Long.MAX_VALUE;
            e.expired = t.getBoolean("expired");
            l.entries.put(e.id, e);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("nextId", nextId);
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("id", e.id);
            t.putString("dim", e.dimension.location().toString());
            t.put("pos", NbtUtils.writeBlockPos(e.pos));
            t.putString("kind", e.kind.name());
            if (e.entity != null) t.putUUID("entity", e.entity);
            if (e.placed != null) t.put("placed", NbtUtils.writeBlockState(e.placed));
            t.putString("occ", e.occurrenceKey);
            t.putString("village", e.villageKey);
            t.putString("piece", e.pieceId);
            if (e.expiresDay != Long.MAX_VALUE) t.putLong("expires", e.expiresDay);
            if (e.expired) t.putBoolean("expired", true);
            list.add(t);
        }
        tag.put("entries", list);
        return tag;
    }

    // ------------------------------------------------------------------ placement

    /** True if {@code pos} is free for a decoration (air with a sturdy block below, no other decoration within 1). */
    public boolean isFreeSpot(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()) return false;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) return false;
        if (!level.getFluidState(pos).isEmpty()) return false;
        for (Entry e : entries.values()) {
            if (e.dimension == level.dimension() && e.pos.distManhattan(pos) <= 1) return false;
        }
        return true;
    }

    /** Spawns the piece's displays at {@code pos} (and its light) and records them. Returns the number of entries. */
    public int place(ServerLevel level, BlockPos pos, DecorationPiece piece, float yaw, String occurrenceKey, String villageKey) {
        int count = 0;
        for (DecorationPiece.Part part : piece.parts()) {
            Display.BlockDisplay display = createDisplay(level, pos, part, yaw);
            if (display == null) continue;
            long id = nextId++;
            display.addTag(ENTITY_TAG);
            CompoundTag data = new CompoundTag();
            data.putLong("ledger", id);
            data.putString("occ", occurrenceKey);
            display.getPersistentData().put(ENTITY_TAG, data);
            // Record first: the entity-join guard rejects decorations the ledger does not know.
            entries.put(id, new Entry(id, level.dimension(), pos, Kind.DISPLAY, display.getUUID(), null, occurrenceKey, villageKey, piece.id()));
            if (!level.addFreshEntity(display)) {
                entries.remove(id);
                continue;
            }
            count++;
        }
        if (piece.lightLevel() > 0 && level.getBlockState(pos).isAir()) {
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.min(15, piece.lightLevel()));
            if (level.setBlock(pos, light, 3)) {
                long id = nextId++;
                entries.put(id, new Entry(id, level.dimension(), pos, Kind.LIGHT_BLOCK, null, light, occurrenceKey, villageKey, piece.id()));
                count++;
            }
        }
        if (count > 0) setDirty();
        return count;
    }

    @Nullable
    private static Display.BlockDisplay createDisplay(ServerLevel level, BlockPos pos, DecorationPiece.Part part, float yaw) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(level);
        if (d == null) return null;
        // Display setters are private; configure through the entity's documented NBT format.
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.put("block_state", NbtUtils.writeBlockState(part.state()));
        Vector3f scale = part.scale();
        // Rotate the part's offset around the decoration centre by yaw, then centre the scaled block on it.
        double rad = Math.toRadians(yaw);
        float ox = (float) (part.offset().x() * Math.cos(rad) - part.offset().z() * Math.sin(rad));
        float oz = (float) (part.offset().x() * Math.sin(rad) + part.offset().z() * Math.cos(rad));
        Quaternionf rot = new Quaternionf().rotationY((float) -rad);
        Vector3f translation = new Vector3f(ox, part.offset().y(), oz)
                .add(new Vector3f(-scale.x() / 2f, 0, -scale.z() / 2f).rotate(rot));
        Transformation t = new Transformation(translation, rot, scale, new Quaternionf());
        DataResult<Tag> encoded = Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, t);
        encoded.result().ifPresent(nbt -> tag.put("transformation", nbt));
        tag.putFloat("view_range", 0.6f);
        d.load(tag);
        d.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        return d;
    }

    // ------------------------------------------------------------------ lifecycle

    /** Schedules every entry of an occurrence to disappear gradually from {@code cleanupStartDay}. */
    public void scheduleExpiry(String occurrenceKey, long cleanupStartDay, int cleanupDays) {
        for (Entry e : entries.values()) {
            if (e.occurrenceKey.equals(occurrenceKey) && e.expiresDay == Long.MAX_VALUE) {
                // Parts of the same decoration (same position) share a seed so they vanish together.
                e.expiresDay = DecorationSchedule.expiryDay(cleanupStartDay, cleanupDays, e.pos.asLong());
            }
        }
        setDirty();
    }

    /** Marks every entry of an occurrence for removal as soon as its chunk is loaded. */
    public void expireOccurrence(String occurrenceKey) {
        boolean any = false;
        for (Entry e : entries.values()) {
            if (e.occurrenceKey.equals(occurrenceKey) && !e.expired) {
                e.expired = true;
                any = true;
            }
        }
        if (any) setDirty();
    }

    /** Processes up to {@code budget} entries; removes due ones whose chunks are loaded. Returns entries removed. */
    public int tick(MinecraftServer server, long today, int budget) {
        if (entries.isEmpty()) return 0;
        if (cursor == null || !cursor.hasNext()) cursor = new ArrayList<>(entries.values()).iterator();
        int removed = 0;
        for (int i = 0; i < budget && cursor.hasNext(); i++) {
            Entry e = cursor.next();
            if (!entries.containsKey(e.id) || !e.isDue(today)) continue;
            ServerLevel level = server.getLevel(e.dimension);
            if (level == null) {
                entries.remove(e.id);
                continue;
            }
            if (!level.isLoaded(e.pos)) continue;
            removeNow(level, e);
            removed++;
        }
        if (removed > 0) setDirty();
        return removed;
    }

    /** Undoes one entry in a loaded chunk. Never touches a block that is no longer exactly what EverFest placed. */
    public void removeNow(ServerLevel level, Entry e) {
        switch (e.kind) {
            case DISPLAY -> {
                Entity entity = e.entity == null ? null : level.getEntity(e.entity);
                if (entity != null) entity.discard();
            }
            case LIGHT_BLOCK -> {
                if (e.placed != null && level.getBlockState(e.pos).equals(e.placed)) {
                    level.setBlock(e.pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        entries.remove(e.id);
        setDirty();
    }

    /** Removes all entries of one decoration (same position, village and occurrence) - used when a villager tidies up. */
    public int removeDecorationAt(ServerLevel level, BlockPos pos, String occurrenceKey) {
        List<Entry> matching = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (e.dimension == level.dimension() && e.pos.equals(pos) && e.occurrenceKey.equals(occurrenceKey)) matching.add(e);
        }
        for (Entry e : matching) removeNow(level, e);
        return matching.size();
    }

    /**
     * Called when a decoration entity is about to join a level. Returns true if it must be discarded: unknown to the
     * ledger (lost ledger, duplicated entity) or already due.
     */
    public boolean shouldRejectEntity(Entity entity, long today) {
        if (!entity.getTags().contains(ENTITY_TAG)) return false;
        CompoundTag data = entity.getPersistentData().getCompound(ENTITY_TAG);
        Entry e = entries.get(data.getLong("ledger"));
        if (e == null || e.entity == null || !e.entity.equals(entity.getUUID())) return true;
        if (e.isDue(today)) {
            entries.remove(e.id);
            setDirty();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ queries

    public int decorationsFor(String villageKey, String occurrenceKey) {
        java.util.Set<Long> positions = new java.util.HashSet<>();
        for (Entry e : entries.values()) {
            if (e.villageKey.equals(villageKey) && e.occurrenceKey.equals(occurrenceKey)) positions.add(e.pos.asLong());
        }
        return positions.size();
    }

    public boolean hasPiece(String villageKey, String occurrenceKey, String pieceId) {
        for (Entry e : entries.values()) {
            if (e.villageKey.equals(villageKey) && e.occurrenceKey.equals(occurrenceKey) && e.pieceId.equals(pieceId)) return true;
        }
        return false;
    }

    /** A loaded decoration of this village whose removal is due or scheduled, for villagers to tidy up. */
    @Nullable
    public Entry findTidyTarget(ServerLevel level, String villageKey, BlockPos near, int radius) {
        Entry best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entry e : entries.values()) {
            if (e.dimension != level.dimension() || !e.villageKey.equals(villageKey) || e.expiresDay == Long.MAX_VALUE) continue;
            double d = e.pos.distSqr(near);
            if (d < bestDist && d <= (double) radius * radius && level.isLoaded(e.pos)) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    public int size() {
        return entries.size();
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }

    public void logSummary() {
        EverFest.LOGGER.info("EverFest decoration ledger: {} entries", entries.size());
    }
}
