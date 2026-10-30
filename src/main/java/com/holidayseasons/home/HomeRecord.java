package com.holidayseasons.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** A player home registered with a Holiday Hearth. */
public final class HomeRecord {
    public enum CandyPolicy { EVERYONE, OWNER_ONLY }

    public final UUID id;
    public final UUID owner;
    String ownerName;
    String name;
    public final ResourceKey<Level> dimension;
    public final BlockPos hearth;
    BlockPos entrance;
    boolean acceptVisitors = true;
    boolean allowVillagerDecorations = false;
    CandyPolicy candyPolicy = CandyPolicy.EVERYONE;
    int visitsThisYear;

    public HomeRecord(UUID id, UUID owner, String ownerName, String name, ResourceKey<Level> dimension, BlockPos hearth, BlockPos entrance) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.name = name;
        this.dimension = dimension;
        this.hearth = hearth;
        this.entrance = entrance;
    }

    public String name() {
        return name;
    }

    public String ownerName() {
        return ownerName;
    }

    public BlockPos entrance() {
        return entrance;
    }

    public boolean acceptVisitors() {
        return acceptVisitors;
    }

    public boolean allowVillagerDecorations() {
        return allowVillagerDecorations;
    }

    public CandyPolicy candyPolicy() {
        return candyPolicy;
    }

    public int visitsThisYear() {
        return visitsThisYear;
    }

    public void applySettings(String name, boolean acceptVisitors, boolean allowVillagerDecorations, CandyPolicy policy) {
        String trimmed = name == null ? "" : name.strip();
        if (!trimmed.isEmpty()) this.name = trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
        this.acceptVisitors = acceptVisitors;
        this.allowVillagerDecorations = allowVillagerDecorations;
        this.candyPolicy = policy;
    }

    void setEntrance(BlockPos entrance) {
        this.entrance = entrance;
    }

    public void countVisit() {
        visitsThisYear++;
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putUUID("owner", owner);
        t.putString("ownerName", ownerName);
        t.putString("name", name);
        t.putString("dim", dimension.location().toString());
        t.put("hearth", NbtUtils.writeBlockPos(hearth));
        t.put("entrance", NbtUtils.writeBlockPos(entrance));
        t.putBoolean("acceptVisitors", acceptVisitors);
        t.putBoolean("villagerDecorations", allowVillagerDecorations);
        t.putString("candyPolicy", candyPolicy.name());
        t.putInt("visits", visitsThisYear);
        return t;
    }

    static HomeRecord load(CompoundTag t) {
        HomeRecord r = new HomeRecord(t.getUUID("id"), t.getUUID("owner"), t.getString("ownerName"), t.getString("name"),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim"))),
                NbtUtils.readBlockPos(t, "hearth").orElse(BlockPos.ZERO), NbtUtils.readBlockPos(t, "entrance").orElse(BlockPos.ZERO));
        r.acceptVisitors = t.getBoolean("acceptVisitors");
        r.allowVillagerDecorations = t.getBoolean("villagerDecorations");
        try {
            r.candyPolicy = CandyPolicy.valueOf(t.getString("candyPolicy"));
        } catch (IllegalArgumentException e) {
            r.candyPolicy = CandyPolicy.EVERYONE;
        }
        r.visitsThisYear = t.getInt("visits");
        return r;
    }
}
