package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.block.EverFestBlocks;
import com.holidayseasons.worldgen.EverFestWorldgen;
import com.holidayseasons.worldgen.HolidayLandmarkStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Tests for EverFest's world-generated holiday landmarks. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGameTests {
    private WorldGameTests() {
    }

    private static List<StructureTemplate.StructureBlockInfo> blocks(GameTestHelper helper, String template, Block block) {
        StructureTemplate t = helper.getLevel().getStructureManager().getOrCreate(EverFest.id("landmark/" + template));
        helper.assertTrue(t.getSize().getX() > 0, template + " template exists");
        return t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block);
    }

    private static void assertLootChest(GameTestHelper helper, String template) {
        List<StructureTemplate.StructureBlockInfo> chests = blocks(helper, template, Blocks.CHEST);
        helper.assertTrue(chests.size() == 1, template + ": one chest, found " + chests.size());
        String table = chests.get(0).nbt() == null ? "" : chests.get(0).nbt().getString("LootTable");
        helper.assertTrue(table.equals("holidayseasons:chests/" + template), template + " chest loot table: " + table);
        var key = ResourceKey.create(Registries.LOOT_TABLE, EverFest.id("chests/" + template));
        LootTable loot = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
        helper.assertTrue(loot != LootTable.EMPTY, template + " loot table loads");
    }

    @GameTest(template = "empty_5x5")
    public static void landmarkTemplatesHaveTheirKeyPieces(GameTestHelper helper) {
        assertLootChest(helper, "seasonal_cabin");
        assertLootChest(helper, "haunted_cottage");
        assertLootChest(helper, "forgotten_graveyard");
        helper.assertTrue(blocks(helper, "seasonal_cabin", EverFestBlocks.HOLIDAY_HEARTH.get()).size() == 1, "cabin has a claimable hearth");
        helper.assertTrue(blocks(helper, "seasonal_cabin", EverFestBlocks.CHRISTMAS_TREE.get()).size() == 2, "cabin has a two-block tree");
        helper.assertTrue(blocks(helper, "seasonal_cabin", Blocks.CAMPFIRE).size() == 1, "cabin chimney smokes");
        helper.assertTrue(blocks(helper, "haunted_cottage", Blocks.COBWEB).size() >= 5, "cottage is cobwebbed");
        helper.assertTrue(blocks(helper, "forgotten_graveyard", EverFestBlocks.TOMBSTONE.get()).size() >= 8, "graveyard has graves");
        helper.succeed();
    }

    @GameTest(template = "empty_5x5")
    public static void landmarksAreRegisteredAndFollowTheConfig(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (ResourceKey<Structure> key : List.of(EverFestWorldgen.HAUNTED_COTTAGE, EverFestWorldgen.FORGOTTEN_GRAVEYARD, EverFestWorldgen.SEASONAL_CABIN)) {
            Structure s = registry.get(key);
            helper.assertTrue(s instanceof HolidayLandmarkStructure, key.location() + " is registered as a holiday landmark");
        }
        var cfg = com.holidayseasons.config.ServerConfig.INSTANCE;
        helper.assertTrue(HolidayLandmarkStructure.enabled("seasonal_cabin") == cfg.seasonalCabins.get(), "cabin switch");
        helper.assertTrue(HolidayLandmarkStructure.enabled("haunted_cottage") == cfg.hauntedCottages.get(), "cottage switch");
        helper.assertTrue(HolidayLandmarkStructure.enabled("forgotten_graveyard") == cfg.forgottenGraveyards.get(), "graveyard switch");
        helper.succeed();
    }
}
