package com.holidayseasons.decoration;

import com.holidayseasons.decoration.DecorationPiece.Part;
import com.holidayseasons.decoration.DecorationPiece.Placement;
import com.holidayseasons.village.VillageStyle;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Decoration sets per holiday theme ({@code decorations} in holiday JSON), adapted to the village's biome style.
 * Every piece is made of block displays, so decorating never replaces or breaks a world block.
 */
public final class DecorationThemes {
    private DecorationThemes() {
    }

    public static List<DecorationPiece> pieces(String theme, VillageStyle style) {
        return switch (theme) {
            case "halloween" -> halloween(style);
            case "christmas" -> christmas(style);
            case "easter" -> easter(style);
            case "new_years" -> newYears(style);
            case "valentines" -> valentines(style);
            case "harvest" -> harvest(style);
            case "independence_day" -> independence(style);
            default -> List.of();
        };
    }

    public static List<DecorationPiece> ground(String theme, VillageStyle style) {
        return pieces(theme, style).stream().filter(p -> p.placement() == Placement.GROUND).toList();
    }

    public static List<DecorationPiece> centre(String theme, VillageStyle style) {
        return pieces(theme, style).stream().filter(p -> p.placement() == Placement.FESTIVAL_CENTER).toList();
    }

    public static DecorationPiece random(List<DecorationPiece> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    // ------------------------------------------------------------------ themes

    private static List<DecorationPiece> halloween(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("jack_o_lantern", 12, Placement.GROUND, Items.JACK_O_LANTERN, Part.of(facing(Blocks.JACK_O_LANTERN, Direction.SOUTH), 0.8f)));
        out.add(piece("carved_pumpkin", 0, Placement.GROUND, Items.CARVED_PUMPKIN, Part.of(facing(Blocks.CARVED_PUMPKIN, Direction.EAST), 0.7f)));
        out.add(piece("pumpkin_patch", 10, Placement.GROUND, Items.PUMPKIN,
                Part.at(Blocks.PUMPKIN.defaultBlockState(), -0.25f, 0, -0.2f, 0.55f),
                Part.at(facing(Blocks.JACK_O_LANTERN, Direction.SOUTH), 0.25f, 0, 0.15f, 0.45f),
                Part.at(Blocks.PUMPKIN.defaultBlockState(), 0.05f, 0, -0.35f, 0.35f)));
        out.add(piece("bale_and_lantern", 13, Placement.GROUND, new ItemStack(s.bale),
                Part.of(s.bale.defaultBlockState(), 0.85f),
                Part.at(lightBlock(s), 0, 0.85f, 0, 0.7f)));
        out.add(piece("cobwebs", 0, Placement.GROUND, Items.COBWEB, Part.of(Blocks.COBWEB.defaultBlockState(), 1f)));
        out.add(piece("skull", 0, Placement.GROUND, Items.SKELETON_SKULL, Part.of(Blocks.SKELETON_SKULL.defaultBlockState(), 1f)));
        out.add(piece("candles", 9, Placement.GROUND, Items.CANDLE, Part.of(candles(Blocks.BLACK_CANDLE, 3), 1f)));
        // Festival square: a heap of pumpkins and lanterns around a scarecrow post.
        out.add(piece("pumpkin_heap", 15, Placement.FESTIVAL_CENTER, Items.JACK_O_LANTERN,
                Part.at(s.bale.defaultBlockState(), 0, 0, 0, 1f),
                Part.at(facing(Blocks.JACK_O_LANTERN, Direction.SOUTH), 0, 1f, 0, 0.8f),
                Part.at(Blocks.PUMPKIN.defaultBlockState(), 0.9f, 0, 0.2f, 0.6f),
                Part.at(facing(Blocks.CARVED_PUMPKIN, Direction.WEST), -0.8f, 0, -0.3f, 0.6f),
                Part.at(Blocks.PUMPKIN.defaultBlockState(), 0.3f, 0, 0.9f, 0.5f)));
        out.add(piece("scarecrow", 0, Placement.FESTIVAL_CENTER, Items.HAY_BLOCK,
                Part.box(s.fence.defaultBlockState(), 0, 0, 0, 0.35f, 1.6f, 0.35f),
                Part.box(Blocks.HAY_BLOCK.defaultBlockState(), 0, 1.1f, 0, 0.55f, 0.55f, 0.4f),
                Part.box(s.log.defaultBlockState(), 0, 1.35f, 0, 1.4f, 0.15f, 0.15f),
                Part.at(facing(Blocks.CARVED_PUMPKIN, Direction.SOUTH), 0, 1.65f, 0, 0.5f)));
        return out;
    }

    private static List<DecorationPiece> christmas(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("lantern", 15, Placement.GROUND, new ItemStack(s.light), Part.of(lightBlock(s), 1f)));
        out.add(piece("present_stack", 0, Placement.GROUND, Items.RED_WOOL,
                Part.at(Blocks.RED_WOOL.defaultBlockState(), -0.15f, 0, 0, 0.45f),
                Part.box(Blocks.YELLOW_WOOL.defaultBlockState(), -0.15f, 0, 0, 0.47f, 0.47f, 0.08f),
                Part.at(Blocks.GREEN_WOOL.defaultBlockState(), 0.25f, 0, 0.2f, 0.35f),
                Part.box(Blocks.WHITE_WOOL.defaultBlockState(), 0.25f, 0, 0.2f, 0.07f, 0.37f, 0.37f),
                Part.at(Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), -0.1f, 0.45f, 0, 0.3f)));
        out.add(piece("candy_cane", 0, Placement.GROUND, Items.RED_WOOL, candyCane()));
        out.add(piece("snowy_shrub", 6, Placement.GROUND, new ItemStack(s.leaves),
                Part.of(leaves(s.leaves), 0.8f),
                Part.at(Blocks.SNOW.defaultBlockState(), 0, 0.8f, 0, 0.8f),
                Part.at(Blocks.GLOWSTONE.defaultBlockState(), 0.3f, 0.5f, 0.38f, 0.12f),
                Part.at(Blocks.REDSTONE_BLOCK.defaultBlockState(), -0.35f, 0.3f, 0.3f, 0.12f)));
        out.add(piece("christmas_tree", 15, Placement.FESTIVAL_CENTER, Items.SPRUCE_SAPLING, tree()));
        return out;
    }

    private static List<DecorationPiece> easter(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("egg_nest", 0, Placement.GROUND, Items.EGG,
                Part.box(Blocks.HAY_BLOCK.defaultBlockState(), 0, 0, 0, 0.7f, 0.15f, 0.7f),
                egg(Blocks.PINK_TERRACOTTA, -0.15f, 0.15f, 0),
                egg(Blocks.LIGHT_BLUE_TERRACOTTA, 0.15f, 0.15f, 0.1f),
                egg(Blocks.YELLOW_TERRACOTTA, 0, 0.15f, -0.18f)));
        out.add(piece("flowers", 0, Placement.GROUND, Items.PINK_TULIP,
                Part.at(Blocks.PINK_TULIP.defaultBlockState(), -0.2f, 0, 0, 1f),
                Part.at(s.flower.defaultBlockState(), 0.25f, 0, 0.2f, 0.9f)));
        out.add(piece("petals", 0, Placement.GROUND, Items.PINK_PETALS, Part.of(Blocks.PINK_PETALS.defaultBlockState()
                .setValue(BlockStateProperties.FLOWER_AMOUNT, 4), 1f)));
        out.add(piece("flower_arch", 8, Placement.FESTIVAL_CENTER, Items.FLOWERING_AZALEA,
                Part.box(s.fence.defaultBlockState(), -1f, 0, 0, 0.3f, 2.2f, 0.3f),
                Part.box(s.fence.defaultBlockState(), 1f, 0, 0, 0.3f, 2.2f, 0.3f),
                Part.box(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), 0, 2.1f, 0, 2.6f, 0.4f, 0.5f),
                Part.at(Blocks.PINK_TULIP.defaultBlockState(), -0.5f, 2.4f, 0, 0.6f),
                Part.at(Blocks.ALLIUM.defaultBlockState(), 0.5f, 2.4f, 0, 0.6f)));
        return out;
    }

    private static List<DecorationPiece> newYears(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("lantern", 15, Placement.GROUND, new ItemStack(s.light), Part.of(lightBlock(s), 1f)));
        out.add(piece("gold_lights", 12, Placement.GROUND, Items.GLOWSTONE,
                Part.box(s.fence.defaultBlockState(), 0, 0, 0, 0.2f, 1.4f, 0.2f),
                Part.at(Blocks.GLOWSTONE.defaultBlockState(), 0, 1.4f, 0, 0.3f),
                Part.at(Blocks.GOLD_BLOCK.defaultBlockState(), 0, 1.7f, 0, 0.15f)));
        out.add(piece("bell_tower_lights", 15, Placement.FESTIVAL_CENTER, Items.LANTERN,
                Part.box(s.log.defaultBlockState(), 0, 0, 0, 0.3f, 3f, 0.3f),
                Part.at(Blocks.SEA_LANTERN.defaultBlockState(), 0, 3f, 0, 0.6f),
                Part.at(Blocks.GOLD_BLOCK.defaultBlockState(), 0, 3.6f, 0, 0.25f)));
        return out;
    }

    private static List<DecorationPiece> valentines(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("heart", 0, Placement.GROUND, Items.RED_WOOL, heart(Blocks.RED_WOOL, 0.18f, 0.2f)));
        out.add(piece("pink_heart", 0, Placement.GROUND, Items.PINK_WOOL, heart(Blocks.PINK_WOOL, 0.14f, 0.15f)));
        out.add(piece("roses", 0, Placement.GROUND, Items.ROSE_BUSH, Part.of(Blocks.POPPY.defaultBlockState(), 1f),
                Part.at(Blocks.PINK_TULIP.defaultBlockState(), 0.3f, 0, 0.2f, 0.8f)));
        out.add(piece("candles", 9, Placement.GROUND, Items.PINK_CANDLE, Part.of(candles(Blocks.PINK_CANDLE, 4), 1f)));
        out.add(piece("big_heart", 10, Placement.FESTIVAL_CENTER, Items.RED_WOOL, heart(Blocks.RED_WOOL, 0.45f, 0.6f)));
        return out;
    }

    private static List<DecorationPiece> harvest(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("bale", 0, Placement.GROUND, new ItemStack(s.bale), Part.of(s.bale.defaultBlockState(), 0.9f)));
        out.add(piece("pumpkins", 0, Placement.GROUND, Items.PUMPKIN, Part.of(Blocks.PUMPKIN.defaultBlockState(), 0.6f),
                Part.at(Blocks.MELON.defaultBlockState(), 0.4f, 0, 0.3f, 0.4f)));
        out.add(piece("wheat_sheaf", 0, Placement.GROUND, Items.WHEAT, Part.of(Blocks.WHEAT.defaultBlockState()
                .setValue(BlockStateProperties.AGE_7, 7), 1f)));
        out.add(piece("barrel", 0, Placement.GROUND, Items.BARREL, Part.of(Blocks.BARREL.defaultBlockState(), 0.8f)));
        out.add(piece("feast_table", 13, Placement.FESTIVAL_CENTER, Items.CAKE,
                Part.box(s.log.defaultBlockState(), 0, 0.7f, 0, 2.4f, 0.15f, 1f),
                Part.box(s.fence.defaultBlockState(), -1f, 0, 0, 0.2f, 0.7f, 0.2f),
                Part.box(s.fence.defaultBlockState(), 1f, 0, 0, 0.2f, 0.7f, 0.2f),
                Part.at(Blocks.CAKE.defaultBlockState(), 0, 0.85f, 0, 0.6f),
                Part.at(Blocks.PUMPKIN.defaultBlockState(), 0.7f, 0.85f, 0.1f, 0.35f),
                Part.at(Blocks.MELON.defaultBlockState(), -0.7f, 0.85f, -0.1f, 0.35f),
                Part.at(lightBlock(s), 0.25f, 0.85f, 0.3f, 0.4f)));
        return out;
    }

    private static List<DecorationPiece> independence(VillageStyle s) {
        List<DecorationPiece> out = new ArrayList<>();
        out.add(piece("flag", 0, Placement.GROUND, Items.WHITE_BANNER, flag()));
        out.add(piece("lantern", 15, Placement.GROUND, new ItemStack(s.light), Part.of(lightBlock(s), 1f)));
        out.add(piece("bunting_post", 0, Placement.GROUND, Items.BLUE_WOOL,
                Part.box(s.fence.defaultBlockState(), 0, 0, 0, 0.2f, 1.6f, 0.2f),
                Part.at(Blocks.RED_WOOL.defaultBlockState(), 0, 1.6f, 0, 0.25f),
                Part.at(Blocks.WHITE_WOOL.defaultBlockState(), 0, 1.85f, 0, 0.2f),
                Part.at(Blocks.BLUE_WOOL.defaultBlockState(), 0, 2.05f, 0, 0.15f)));
        out.add(piece("big_flag", 0, Placement.FESTIVAL_CENTER, Items.WHITE_BANNER, flag()));
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static DecorationPiece piece(String id, int light, Placement placement, Object carried, Part... parts) {
        ItemStack stack = carried instanceof ItemStack is ? is : new ItemStack((net.minecraft.world.level.ItemLike) carried);
        return new DecorationPiece(id, List.of(parts), light, placement, stack);
    }

    private static BlockState facing(Block block, Direction dir) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    private static BlockState lightBlock(VillageStyle s) {
        BlockState state = s.light.defaultBlockState();
        if (s.light instanceof CandleBlock) return candles(s.light, 3);
        return state;
    }

    private static BlockState candles(Block candle, int count) {
        return candle.defaultBlockState().setValue(CandleBlock.CANDLES, count).setValue(CandleBlock.LIT, true);
    }

    private static BlockState leaves(Block leaves) {
        BlockState state = leaves.defaultBlockState();
        return state.hasProperty(BlockStateProperties.PERSISTENT) ? state.setValue(BlockStateProperties.PERSISTENT, true) : state;
    }

    private static Part egg(Block block, float x, float y, float z) {
        return Part.box(block.defaultBlockState(), x, y, z, 0.18f, 0.24f, 0.18f);
    }

    private static Part[] candyCane() {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Block b = i % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE;
            parts.add(Part.box(b.defaultBlockState(), 0, i * 0.2f, 0, 0.14f, 0.2f, 0.14f));
        }
        parts.add(Part.box(Blocks.WHITE_CONCRETE.defaultBlockState(), 0.12f, 1.4f, 0, 0.3f, 0.14f, 0.14f));
        parts.add(Part.box(Blocks.RED_CONCRETE.defaultBlockState(), 0.27f, 1.2f, 0, 0.14f, 0.2f, 0.14f));
        return parts.toArray(Part[]::new);
    }

    private static Part[] tree() {
        List<Part> parts = new ArrayList<>();
        BlockState leaves = leaves(Blocks.SPRUCE_LEAVES);
        parts.add(Part.box(Blocks.SPRUCE_LOG.defaultBlockState(), 0, 0, 0, 0.4f, 1f, 0.4f));
        float[][] layers = {{0.6f, 2.6f}, {1.4f, 2.0f}, {2.2f, 1.4f}, {2.9f, 0.8f}};
        for (float[] l : layers) parts.add(Part.box(leaves, 0, l[0], 0, l[1], 0.8f, l[1]));
        parts.add(Part.at(Blocks.GLOWSTONE.defaultBlockState(), 0, 3.65f, 0, 0.35f));
        Block[] ornaments = {Blocks.RED_CONCRETE, Blocks.GOLD_BLOCK, Blocks.LIGHT_BLUE_CONCRETE, Blocks.RED_CONCRETE, Blocks.GOLD_BLOCK, Blocks.MAGENTA_CONCRETE};
        Random r = new Random(25L);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 6 + (i >= 6 ? 0.5 : 0);
            float layerY = i < 6 ? 0.9f : 1.7f;
            float radius = i < 6 ? 1.3f : 0.95f;
            parts.add(Part.at(ornaments[i % ornaments.length].defaultBlockState(), (float) Math.cos(a) * radius,
                    layerY + r.nextFloat() * 0.2f, (float) Math.sin(a) * radius, 0.16f));
        }
        parts.add(Part.at(Blocks.RED_WOOL.defaultBlockState(), 0.9f, 0, 0.6f, 0.4f));
        parts.add(Part.at(Blocks.GREEN_WOOL.defaultBlockState(), -0.8f, 0, 0.7f, 0.35f));
        parts.add(Part.at(Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), 0.1f, 0, -1f, 0.45f));
        return parts.toArray(Part[]::new);
    }

    private static Part[] heart(Block block, float cell, float lift) {
        // 5x5 pixel heart standing upright on the X/Y plane.
        String[] rows = {".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X.."};
        List<Part> parts = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < 5; col++) {
                if (rows[row].charAt(col) != 'X') continue;
                float x = (col - 2) * cell;
                float y = lift + (rows.length - 1 - row) * cell;
                parts.add(Part.box(block.defaultBlockState(), x, y, 0, cell, cell, cell * 0.6f));
            }
        }
        parts.add(Part.box(Blocks.OAK_FENCE.defaultBlockState(), 0, 0, 0, 0.12f, lift, 0.12f));
        return parts.toArray(Part[]::new);
    }

    private static Part[] flag() {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.box(Blocks.WHITE_CONCRETE.defaultBlockState(), 0, 0, 0, 0.12f, 2.6f, 0.12f));
        for (int i = 0; i < 6; i++) {
            Block b = i % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE;
            parts.add(Part.box(b.defaultBlockState(), 0.65f, 2.45f - i * 0.15f, 0, 1.1f, 0.15f, 0.04f));
        }
        parts.add(Part.box(Blocks.BLUE_CONCRETE.defaultBlockState(), 0.33f, 2.25f, 0.01f, 0.45f, 0.45f, 0.05f));
        return parts.toArray(Part[]::new);
    }
}
