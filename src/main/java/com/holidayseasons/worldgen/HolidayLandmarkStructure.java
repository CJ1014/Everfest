package com.holidayseasons.worldgen;

import com.holidayseasons.config.ServerConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * A single-template holiday landmark (haunted cottage, forgotten graveyard, seasonal cabin). It only generates on
 * dry, fairly flat ground, sits its floor on the lowest corner of its footprint and relies on the structure's terrain
 * adaptation (beard) to blend the ground in, so it is never left floating or buried. Each landmark can be switched off
 * in the server config ({@code worldgen} section); data packs can retune where and how often it appears.
 */
public class HolidayLandmarkStructure extends Structure {
    public static final MapCodec<HolidayLandmarkStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.STRING.fieldOf("config_key").forGetter(s -> s.configKey),
            Codec.intRange(0, 16).optionalFieldOf("max_slope", 4).forGetter(s -> s.maxSlope)
    ).apply(i, HolidayLandmarkStructure::new));

    private final ResourceLocation template;
    private final String configKey;
    private final int maxSlope;

    public HolidayLandmarkStructure(StructureSettings settings, ResourceLocation template, String configKey, int maxSlope) {
        super(settings);
        this.template = template;
        this.configKey = configKey;
        this.maxSlope = maxSlope;
    }

    /** Whether the server config allows this landmark (true while no config is loaded, e.g. during data generation). */
    public static boolean enabled(String key) {
        if (!ServerConfig.SPEC.isLoaded()) return true;
        return switch (key) {
            case "haunted_cottage" -> ServerConfig.INSTANCE.hauntedCottages.get();
            case "forgotten_graveyard" -> ServerConfig.INSTANCE.forgottenGraveyards.get();
            case "seasonal_cabin" -> ServerConfig.INSTANCE.seasonalCabins.get();
            default -> true;
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!enabled(configKey)) return Optional.empty();
        Rotation rotation = Rotation.getRandom(ctx.random());
        StructureTemplate tpl = ctx.structureTemplateManager().getOrCreate(template);
        ChunkPos chunk = ctx.chunkPos();
        // Place the template so its rotated footprint starts at the chunk corner.
        BoundingBox local = tpl.getBoundingBox(HolidayLandmarkPiece.settings(rotation), BlockPos.ZERO);
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        int x1 = x0 + local.getXSpan() - 1;
        int z1 = z0 + local.getZSpan() - 1;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int[][] samples = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}};
        for (int[] s : samples) {
            int surface = ctx.chunkGenerator().getFirstOccupiedHeight(s[0], s[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int floor = ctx.chunkGenerator().getFirstOccupiedHeight(s[0], s[1], Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
            if (floor < surface) return Optional.empty(); // water
            min = Math.min(min, surface);
            max = Math.max(max, surface);
        }
        if (max - min > maxSlope || min <= ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        // The template's first layer is its foundation: it replaces the top block of the ground.
        BlockPos pos = new BlockPos(x0 - local.minX(), min - 1, z0 - local.minZ());
        return Optional.of(new GenerationStub(new BlockPos((x0 + x1) / 2, min, (z0 + z1) / 2),
                builder -> builder.addPiece(new HolidayLandmarkPiece(ctx.structureTemplateManager(), template, pos, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return EverFestWorldgen.HOLIDAY_LANDMARK.get();
    }
}
