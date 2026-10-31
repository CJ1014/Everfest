package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.GhostEntity;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import com.holidayseasons.worldgen.EverFestWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Halloween's graveyard encounter: when a player walks into a forgotten graveyard on Halloween night, the graves stir
 * and a few ghosts rise from them. Once per graveyard per night; the ghosts are ordinary Halloween creatures, so they
 * fade at dawn like the others. Nothing is destroyed. Configurable (events section).
 */
public final class GraveyardHaunting {
    public static final int GHOSTS = 3;
    private final EverFestServer services;

    public GraveyardHaunting(EverFestServer services) {
        this.services = services;
    }

    public static boolean enabled() {
        return !ServerConfig.SPEC.isLoaded() || ServerConfig.INSTANCE.graveyardHaunting.get();
    }

    public void tick() {
        ServerLevel level = services.server().overworld();
        if (level.getGameTime() % 40 != 0 || !enabled() || !services.halloween().isHalloweenNight()) return;
        Structure graveyard = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(EverFestWorldgen.FORGOTTEN_GRAVEYARD);
        if (graveyard == null) return;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), graveyard);
            if (start.isValid()) haunt(level, start.getBoundingBox(), start.getChunkPos().toLong(), player);
        }
    }

    /**
     * Raises the ghosts of the graveyard in {@code box} (identified by {@code graveyardId}), telling {@code player} if
     * given. Returns how many rose (0 if it already happened tonight).
     */
    public int haunt(ServerLevel level, BoundingBox box, long graveyardId, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        var f = services.halloween().halloween();
        String occ = f == null ? "none" : f.occurrence().key();
        String claim = "haunting|" + occ + "|" + graveyardId + "|" + services.calendar().schedulingSnapshot().epochDay();
        if (services.gifts().claims().has(claim)) return 0;
        List<BlockPos> graves = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(p).is(EverFestBlocks.TOMBSTONE.get())) graves.add(p.immutable());
        }
        Collections.shuffle(graves, new java.util.Random(level.random.nextLong()));
        int risen = 0;
        for (BlockPos grave : graves.subList(0, Math.min(GHOSTS, graves.size()))) {
            GhostEntity ghost = EverFestEntities.GHOST.get().create(level);
            if (ghost == null) continue;
            ghost.moveTo(grave.getX() + 0.5, grave.getY() + 1.2, grave.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
            ghost.addTag(HalloweenService.CREATURE_TAG);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(ghost, level, level.getCurrentDifficultyAt(grave), MobSpawnType.EVENT, null);
            if (!level.addFreshEntity(ghost)) continue; // refused outside Halloween night
            level.sendParticles(ParticleTypes.SOUL, grave.getX() + 0.5, grave.getY() + 0.8, grave.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.03);
            risen++;
        }
        if (risen > 0) services.gifts().claims().claim(claim);
        if (risen > 0 && player != null) {
            player.displayClientMessage(Component.translatable("holidayseasons.graveyard.stirs"), false);
            player.playNotifySound(EverFestSounds.GHOST_MOAN.get(), SoundSource.HOSTILE, 1f, 0.8f);
            EverFest.LOGGER.info("EverFest: {} ghosts rose in a graveyard near {}", risen, player.getGameProfile().getName());
        }
        return risen;
    }
}
