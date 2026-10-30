package com.holidayseasons.village;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Biome-dependent materials for village celebrations, chosen from the villagers' own {@link VillagerType} so modded
 * village types fall back to plains gracefully.
 */
public enum VillageStyle {
    PLAINS(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.LANTERN, Blocks.HAY_BLOCK, Blocks.POPPY, Blocks.OAK_FENCE),
    DESERT(Blocks.SMOOTH_SANDSTONE, Blocks.JUNGLE_LEAVES, Blocks.CANDLE, Blocks.TERRACOTTA, Blocks.DEAD_BUSH, Blocks.SANDSTONE_WALL),
    SAVANNA(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.LANTERN, Blocks.HAY_BLOCK, Blocks.ORANGE_TULIP, Blocks.ACACIA_FENCE),
    SNOW(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.LANTERN, Blocks.SNOW_BLOCK, Blocks.BLUE_ORCHID, Blocks.SPRUCE_FENCE),
    TAIGA(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.LANTERN, Blocks.HAY_BLOCK, Blocks.SWEET_BERRY_BUSH, Blocks.SPRUCE_FENCE),
    JUNGLE(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.LANTERN, Blocks.BAMBOO_BLOCK, Blocks.ALLIUM, Blocks.BAMBOO_FENCE),
    SWAMP(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Blocks.SOUL_LANTERN, Blocks.MOSS_BLOCK, Blocks.BLUE_ORCHID, Blocks.MANGROVE_FENCE);

    public final Block log;
    public final Block leaves;
    public final Block light;
    public final Block bale;
    public final Block flower;
    public final Block fence;

    VillageStyle(Block log, Block leaves, Block light, Block bale, Block flower, Block fence) {
        this.log = log;
        this.leaves = leaves;
        this.light = light;
        this.bale = bale;
        this.flower = flower;
        this.fence = fence;
    }

    public static VillageStyle of(VillagerType type) {
        String id = BuiltInRegistries.VILLAGER_TYPE.getKey(type).getPath();
        return byId(id);
    }

    public static VillageStyle byId(String id) {
        return switch (id) {
            case "desert" -> DESERT;
            case "savanna" -> SAVANNA;
            case "snow" -> SNOW;
            case "taiga" -> TAIGA;
            case "jungle" -> JUNGLE;
            case "swamp" -> SWAMP;
            default -> PLAINS;
        };
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
