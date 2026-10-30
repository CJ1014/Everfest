package com.holidayseasons.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;

/**
 * Identifies the village a villager belongs to: its meeting point (bell) if it has one, otherwise a 64-block cell
 * around its home or position. Vanilla has no village object, so this mirrors how villagers themselves gather.
 */
public final class VillageKeys {
    public static final int CELL = 64;

    private VillageKeys() {
    }

    public record Resolved(String key, BlockPos centre, boolean hasBell) {
    }

    public static Resolved of(Villager v) {
        String dim = v.level().dimension().location().toString();
        Optional<GlobalPos> bell = v.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
        if (bell.isPresent() && bell.get().dimension() == v.level().dimension()) {
            BlockPos p = bell.get().pos();
            return new Resolved(dim + "|bell|" + p.getX() + "," + p.getY() + "," + p.getZ(), p, true);
        }
        BlockPos anchor = v.getBrain().getMemory(MemoryModuleType.HOME)
                .filter(h -> h.dimension() == v.level().dimension()).map(GlobalPos::pos).orElse(v.blockPosition());
        int cx = Math.floorDiv(anchor.getX(), CELL);
        int cz = Math.floorDiv(anchor.getZ(), CELL);
        // Without a bell the festival point is the villager's home (or where it stands).
        return new Resolved(dim + "|cell|" + cx + "," + cz, anchor, false);
    }
}
