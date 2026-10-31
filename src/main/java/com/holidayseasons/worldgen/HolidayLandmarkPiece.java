package com.holidayseasons.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** The one template piece of a {@link HolidayLandmarkStructure}. */
public class HolidayLandmarkPiece extends TemplateStructurePiece {
    public HolidayLandmarkPiece(StructureTemplateManager templates, ResourceLocation template, BlockPos pos, Rotation rotation) {
        super(EverFestWorldgen.LANDMARK_PIECE.get(), 0, templates, template, template.toString(), settings(rotation), pos);
    }

    public HolidayLandmarkPiece(StructureTemplateManager templates, CompoundTag tag) {
        super(EverFestWorldgen.LANDMARK_PIECE.get(), tag, templates, loc -> settings(Rotation.valueOf(tag.getString("Rot"))));
    }

    static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // The templates carry their chests' loot tables directly; no data markers are used.
    }
}
