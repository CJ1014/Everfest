package com.holidayseasons.home;

import com.holidayseasons.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registered player homes ({@code holidayseasons_homes.dat}). Server-authoritative. */
public final class HomeRegistry extends SavedData {
    public static final String FILE_ID = "holidayseasons_homes";
    public static final Factory<HomeRegistry> FACTORY = new Factory<>(HomeRegistry::new, HomeRegistry::load, null);

    private final Map<UUID, HomeRecord> homes = new LinkedHashMap<>();

    public static HomeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        HomeRegistry r = new HomeRegistry();
        ListTag list = tag.getList("homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            HomeRecord h = HomeRecord.load(list.getCompound(i));
            r.homes.put(h.id, h);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (HomeRecord h : homes.values()) list.add(h.save());
        tag.put("homes", list);
        return tag;
    }

    public static int maxHomesPerPlayer() {
        return ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.maxHomesPerPlayer.get() : 2;
    }

    public static int minDistanceBetweenOwners() {
        return ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.minHomeDistance.get() : 16;
    }

    /** Why a hearth cannot be registered here, or null if it can. */
    @Nullable
    public String validate(UUID owner, ResourceKey<Level> dim, BlockPos pos) {
        int owned = 0;
        int min = minDistanceBetweenOwners();
        for (HomeRecord h : homes.values()) {
            if (h.owner.equals(owner)) owned++;
            if (h.dimension == dim && !h.owner.equals(owner) && h.hearth.distSqr(pos) < (double) min * min) {
                return "holidayseasons.home.too_close";
            }
        }
        if (owned >= maxHomesPerPlayer()) return "holidayseasons.home.too_many";
        return null;
    }

    public HomeRecord register(ServerLevel level, UUID owner, String ownerName, BlockPos hearth) {
        HomeRecord h = new HomeRecord(UUID.randomUUID(), owner, ownerName, ownerName + "'s Home", level.dimension(), hearth,
                findEntrance(level, hearth));
        homes.put(h.id, h);
        setDirty();
        return h;
    }

    public void unregister(UUID id) {
        if (homes.remove(id) != null) setDirty();
    }

    @Nullable
    public HomeRecord get(UUID id) {
        return homes.get(id);
    }

    @Nullable
    public HomeRecord atHearth(ResourceKey<Level> dim, BlockPos pos) {
        for (HomeRecord h : homes.values()) if (h.dimension == dim && h.hearth.equals(pos)) return h;
        return null;
    }

    /** The home whose hearth or entrance is within {@code radius} of {@code pos}. */
    @Nullable
    public HomeRecord near(ResourceKey<Level> dim, BlockPos pos, int radius) {
        HomeRecord best = null;
        double bestDist = (double) radius * radius;
        for (HomeRecord h : homes.values()) {
            if (h.dimension != dim) continue;
            double d = Math.min(h.hearth.distSqr(pos), h.entrance.distSqr(pos));
            if (d <= bestDist) {
                best = h;
                bestDist = d;
            }
        }
        return best;
    }

    /** True if EverFest's villagers must not decorate here (inside a player's home area that did not opt in). */
    public boolean blocksVillagerDecorations(ResourceKey<Level> dim, BlockPos pos) {
        int radius = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.homeProtectionRadius.get() : 12;
        HomeRecord h = near(dim, pos, radius);
        return h != null && !h.allowVillagerDecorations;
    }

    public List<HomeRecord> visitable(ResourceKey<Level> dim, BlockPos centre, int radius) {
        List<HomeRecord> out = new ArrayList<>();
        for (HomeRecord h : homes.values()) {
            if (h.dimension == dim && h.acceptVisitors && h.entrance.distSqr(centre) <= (double) radius * radius) out.add(h);
        }
        return out;
    }

    public List<HomeRecord> ownedBy(UUID owner) {
        List<HomeRecord> out = new ArrayList<>();
        for (HomeRecord h : homes.values()) if (h.owner.equals(owner)) out.add(h);
        return out;
    }

    public Collection<HomeRecord> all() {
        return homes.values();
    }

    public void resetYearlyVisits() {
        homes.values().forEach(h -> h.visitsThisYear = 0);
        setDirty();
    }

    /** Re-detects the entrance (e.g. after the player added a door). */
    public void refreshEntrance(ServerLevel level, HomeRecord h) {
        h.setEntrance(findEntrance(level, h.hearth));
        setDirty();
    }

    /**
     * The spot visitors stand on: outside the nearest door within 10 blocks (the side that sees the sky), or the hearth
     * itself if there is no door.
     */
    public static BlockPos findEntrance(ServerLevel level, BlockPos hearth) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(hearth.offset(-10, -3, -10), hearth.offset(10, 3, 10))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                double d = p.distSqr(hearth);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        if (best == null) return hearth;
        Direction facing = level.getBlockState(best).getValue(DoorBlock.FACING);
        BlockPos front = best.relative(facing);
        BlockPos back = best.relative(facing.getOpposite());
        boolean frontSky = level.canSeeSky(front.above());
        boolean backSky = level.canSeeSky(back.above());
        if (frontSky != backSky) return frontSky ? front : back;
        // Both or neither see the sky: the side farther from the hearth is outside.
        return front.distSqr(hearth) >= back.distSqr(hearth) ? front : back;
    }
}
