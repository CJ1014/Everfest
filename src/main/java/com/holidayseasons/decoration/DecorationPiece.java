package com.holidayseasons.decoration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;

/**
 * One decoration a villager can put up: one or more block-display parts (so it never replaces world blocks),
 * an optional invisible light source, and the item the villager visibly carries while placing it.
 *
 * @param id         stable id used in the ledger
 * @param parts      display parts relative to the decoration's block position (offsets in blocks, from the block's
 *                   bottom-centre)
 * @param lightLevel 0 for none, otherwise a temporary {@code minecraft:light} block is placed if the spot is air
 * @param placement  where the decoration goes
 * @param carried    item shown in the villager's hands while carrying it
 */
public record DecorationPiece(String id, List<Part> parts, int lightLevel, Placement placement, ItemStack carried) {
    public enum Placement { GROUND, FESTIVAL_CENTER }

    /** A block rendered by a block display, scaled per axis and offset from the decoration position. */
    public record Part(BlockState state, Vector3f offset, Vector3f scale, float yaw) {
        public static Part of(BlockState state, float scale) {
            return new Part(state, new Vector3f(0, 0, 0), new Vector3f(scale, scale, scale), 0);
        }

        public static Part at(BlockState state, float x, float y, float z, float scale) {
            return new Part(state, new Vector3f(x, y, z), new Vector3f(scale, scale, scale), 0);
        }

        public static Part box(BlockState state, float x, float y, float z, float sx, float sy, float sz) {
            return new Part(state, new Vector3f(x, y, z), new Vector3f(sx, sy, sz), 0);
        }
    }
}
