package com.holidayseasons.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A visiting merchant's temporary market stall: posts, a striped canopy in the holiday's colours and a counter with a
 * few goods. Built entirely from ledger-tracked block displays (no real blocks), only where a 3x3x3 space is open
 * ground, and removed with the rest of the holiday's decorations.
 */
public final class MarketStalls {
    public static final String PIECE_ID = "market_stall";

    private MarketStalls() {
    }

    static DecorationPiece stall(String holidayPath) {
        Block a;
        Block b;
        List<BlockState> goods = new ArrayList<>();
        switch (holidayPath) {
            case "christmas" -> { a = Blocks.RED_WOOL; b = Blocks.WHITE_WOOL; goods.add(Blocks.RED_WOOL.defaultBlockState()); goods.add(Blocks.SPRUCE_LEAVES.defaultBlockState()); }
            case "halloween" -> { a = Blocks.ORANGE_WOOL; b = Blocks.BLACK_WOOL; goods.add(facing(Blocks.JACK_O_LANTERN)); goods.add(Blocks.PUMPKIN.defaultBlockState()); }
            case "easter" -> { a = Blocks.PINK_WOOL; b = Blocks.LIGHT_BLUE_WOOL; goods.add(Blocks.YELLOW_TERRACOTTA.defaultBlockState()); goods.add(Blocks.LIME_TERRACOTTA.defaultBlockState()); }
            case "valentines" -> { a = Blocks.RED_WOOL; b = Blocks.PINK_WOOL; goods.add(Blocks.CAKE.defaultBlockState()); goods.add(Blocks.RED_WOOL.defaultBlockState()); }
            case "thanksgiving" -> { a = Blocks.ORANGE_WOOL; b = Blocks.BROWN_WOOL; goods.add(Blocks.PUMPKIN.defaultBlockState()); goods.add(Blocks.HAY_BLOCK.defaultBlockState()); }
            default -> { a = Blocks.BLUE_WOOL; b = Blocks.WHITE_WOOL; goods.add(Blocks.CAKE.defaultBlockState()); goods.add(Blocks.BARREL.defaultBlockState()); }
        }
        List<DecorationPiece.Part> parts = new ArrayList<>();
        BlockState post = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (float x : new float[]{-1.1f, 1.1f}) {
            for (float z : new float[]{-0.6f, 0.6f}) parts.add(DecorationPiece.Part.box(post, x, 0, z, 0.2f, 2.1f, 0.2f));
        }
        for (int i = -1; i <= 1; i++) {
            parts.add(DecorationPiece.Part.box((i == 0 ? b : a).defaultBlockState(), i * 0.8f, 2.1f, 0, 0.8f, 0.15f, 1.6f));
        }
        parts.add(DecorationPiece.Part.box(Blocks.SPRUCE_PLANKS.defaultBlockState(), 0, 0, -0.35f, 2.1f, 0.85f, 0.5f));
        parts.add(DecorationPiece.Part.at(goods.get(0), -0.5f, 0.85f, -0.35f, 0.35f));
        parts.add(DecorationPiece.Part.at(goods.get(1), 0.4f, 0.85f, -0.35f, 0.3f));
        return new DecorationPiece(PIECE_ID, List.copyOf(parts), 11, DecorationPiece.Placement.GROUND, new ItemStack(Items.BARREL));
    }

    private static BlockState facing(Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
    }

    /**
     * Whether the stall's 3x3 footprint around {@code pos} is open ground, three blocks tall. Grass and other
     * replaceable plants count as open: the stall is made of displays and never changes a block.
     */
    static boolean roomFor(ServerLevel level, DecorationLedger ledger, BlockPos pos) {
        if (!level.isLoaded(pos) || ledger.decorationNear(level, pos, 3)) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = pos.offset(dx, -1, dz);
                BlockState g = level.getBlockState(ground);
                // Any solid ground will do (paths and farmland too): the stall has no collision of its own.
                if (g.getCollisionShape(level, ground).isEmpty() || !g.getFluidState().isEmpty()) return false;
                for (int dy = 0; dy < 3; dy++) {
                    BlockState s = level.getBlockState(pos.offset(dx, dy, dz));
                    if (!s.isAir() && !(s.canBeReplaced() && s.getFluidState().isEmpty())) return false;
                }
            }
        }
        return true;
    }

    /** Sets up one stall near {@code near} for this holiday and village. Returns where, or null if there was no room. */
    public static BlockPos place(ServerLevel level, DecorationLedger ledger, BlockPos near, String holidayId, String occurrenceKey,
                                 String villageKey, RandomSource random) {
        if (ledger.countPieces(villageKey, occurrenceKey, PIECE_ID) > 0) return null;
        DecorationPiece piece = stall(holidayId.substring(holidayId.indexOf(':') + 1));
        for (int i = 0; i < 48; i++) {
            BlockPos at = near.offset(random.nextInt(13) - 6, random.nextInt(3) - 1, random.nextInt(13) - 6);
            if (!roomFor(level, ledger, at)) continue;
            float yaw = 90f * random.nextInt(4);
            if (ledger.place(level, at, piece, yaw, occurrenceKey, villageKey) > 0) return at;
        }
        return null;
    }
}
