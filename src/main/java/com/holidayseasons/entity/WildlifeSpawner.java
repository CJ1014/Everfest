package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Seasonal animal visits, in the spirit of vanilla's patrol and phantom spawners: every few hundred ticks each player
 * may be visited by a small group of in-season animals 24–48 blocks away, in a suitable biome, on ground where the
 * animal could spawn naturally. A per-species cap around the player keeps the world from filling up; out-of-season
 * animals leave again on their own (see the entities' despawn rules).
 */
public final class WildlifeSpawner {
    public static final TagKey<Biome> BUNNY_BIOMES = TagKey.create(Registries.BIOME, EverFest.id("spawns_spring_bunnies"));
    public static final TagKey<Biome> TURKEY_BIOMES = TagKey.create(Registries.BIOME, EverFest.id("spawns_wild_turkeys"));
    private final EverFestServer services;
    private final Random random = new Random();

    public WildlifeSpawner(EverFestServer services) {
        this.services = services;
    }

    public void tick() {
        if (!ServerConfig.SPEC.isLoaded()) return;
        ServerLevel level = services.server().overworld();
        if (level.getGameTime() % ServerConfig.INSTANCE.seasonalSpawnIntervalTicks.get() != 0) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        for (SeasonalWildlife.Species s : inSeasonNow()) {
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator()) trySpawn(level, p.blockPosition(), s, false);
            }
        }
    }

    public List<SeasonalWildlife.Species> inSeasonNow() {
        var f = services.director().festival();
        boolean festive = f != null && (f.phase() == HolidayPhase.PREPARING || f.phase() == HolidayPhase.ACTIVE);
        String id = festive ? f.holiday().id() : "";
        return SeasonalWildlife.inSeason(services.calendar().snapshot().season(),
                id.equals(EverFest.MOD_ID + ":easter"), id.equals(EverFest.MOD_ID + ":thanksgiving"));
    }

    public static boolean enabled(SeasonalWildlife.Species s) {
        if (!ServerConfig.SPEC.isLoaded()) return true;
        return switch (s) {
            case SPRING_BUNNY -> ServerConfig.INSTANCE.springBunnies.get();
            case WILD_TURKEY -> ServerConfig.INSTANCE.wildTurkeys.get();
        };
    }

    public static EntityType<? extends Mob> typeOf(SeasonalWildlife.Species s) {
        return switch (s) {
            case SPRING_BUNNY -> EverFestEntities.SPRING_BUNNY.get();
            case WILD_TURKEY -> EverFestEntities.WILD_TURKEY.get();
        };
    }

    private static TagKey<Biome> biomesOf(SeasonalWildlife.Species s) {
        return s == SeasonalWildlife.Species.SPRING_BUNNY ? BUNNY_BIOMES : TURKEY_BIOMES;
    }

    public int nearby(ServerLevel level, BlockPos around, SeasonalWildlife.Species s) {
        return level.getEntities(typeOf(s), new AABB(around).inflate(64), e -> e.isAlive()).size();
    }

    /**
     * Tries to bring one group of {@code s} near {@code around}. {@code force} skips only the chance roll (commands and
     * tests); the cap, the season-independent biome check and the vanilla placement rules always apply.
     * Returns how many animals arrived.
     */
    public int trySpawn(ServerLevel level, BlockPos around, SeasonalWildlife.Species s, boolean force) {
        if (!enabled(s)) return 0;
        int cap = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.maxSeasonalAnimalsNearPlayer.get() : 5;
        double chance = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.seasonalSpawnChance.get() : 0.3;
        int nearby = nearby(level, around, s);
        if (!SeasonalWildlife.mayArrive(nearby, cap, random.nextDouble(), chance, force)) return 0;
        EntityType<? extends Mob> type = typeOf(s);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = 24 + random.nextInt(25);
            BlockPos origin = surface(level, around.getX() + Mth.floor(Math.cos(angle) * dist), around.getZ() + Mth.floor(Math.sin(angle) * dist));
            if (origin == null || !level.getBiome(origin).is(biomesOf(s))) continue;
            int spawned = spawnGroupAt(level, origin, s, SeasonalWildlife.groupSize(s, nearby, cap, random));
            if (spawned > 0) {
                EverFest.LOGGER.debug("EverFest: {} {} arrived near {}", spawned, s.id, around);
                return spawned;
            }
        }
        return 0;
    }

    /** Spawns up to {@code group} animals around {@code origin}, each on ground where vanilla rules allow it. */
    public int spawnGroupAt(ServerLevel level, BlockPos origin, SeasonalWildlife.Species s, int group) {
        EntityType<? extends Mob> type = typeOf(s);
        if (!canSpawnAt(level, type, origin)) return 0;
        int spawned = 0;
        for (int i = 0; i < group * 3 && spawned < group; i++) {
            BlockPos at = i == 0 ? origin : SpawnSpots.groundNear(level, origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            if (at == null || !canSpawnAt(level, type, at)) continue;
            Mob mob = type.create(level);
            if (mob == null) break;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
            level.addFreshEntityWithPassengers(mob);
            spawned++;
        }
        return spawned;
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private boolean canSpawnAt(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos)
                && SpawnPlacements.isSpawnPositionOk(type, level, pos)
                && SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.getRandom())
                && level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }
}
