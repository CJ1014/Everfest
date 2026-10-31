package com.holidayseasons.worldgen;

import com.holidayseasons.EverFest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation registrations: the holiday landmark structure type and its piece. */
public final class EverFestWorldgen {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, EverFest.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, EverFest.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<HolidayLandmarkStructure>> HOLIDAY_LANDMARK =
            STRUCTURE_TYPES.register("holiday_landmark", () -> () -> HolidayLandmarkStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LANDMARK_PIECE =
            PIECES.register("holiday_landmark", () -> (StructurePieceType.StructureTemplateType) HolidayLandmarkPiece::new);

    public static final ResourceKey<Structure> HAUNTED_COTTAGE = key("haunted_cottage");
    public static final ResourceKey<Structure> FORGOTTEN_GRAVEYARD = key("forgotten_graveyard");
    public static final ResourceKey<Structure> SEASONAL_CABIN = key("seasonal_cabin");

    private EverFestWorldgen() {
    }

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE, EverFest.id(name));
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
