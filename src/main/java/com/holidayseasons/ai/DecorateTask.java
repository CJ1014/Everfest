package com.holidayseasons.ai;

import com.holidayseasons.decoration.DecorationLedger;
import com.holidayseasons.decoration.DecorationPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntConsumer;

/** Carry a decoration to a spot, look at it for a moment, then put it up. */
public final class DecorateTask extends CelebrationTask {
    private final BlockPos spot;
    private final DecorationPiece piece;
    private final float yaw;
    private final DecorationLedger ledger;
    private final IntConsumer onPlaced;
    private int placingTicks;

    public DecorateTask(String occ, String village, BlockPos spot, DecorationPiece piece, float yaw, DecorationLedger ledger, IntConsumer onPlaced) {
        super(TaskType.DECORATE, occ, village);
        this.spot = spot;
        this.piece = piece;
        this.yaw = yaw;
        this.ledger = ledger;
        this.onPlaced = onPlaced;
    }

    public BlockPos spot() {
        return spot;
    }

    public DecorationPiece piece() {
        return piece;
    }

    @Override
    protected void onStart(ServerLevel level, Villager v) {
        carry(v, piece.carried());
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (!walkTo(v, spot, 0.5f, 2)) return;
        lookAt(v, spot);
        if (++placingTicks < 25) {
            if (placingTicks % 8 == 0) particles(level, Vec3.atBottomCenterOf(spot).add(0, 0.4, 0), ParticleTypes.HAPPY_VILLAGER, 2, 0.3);
            return;
        }
        if (!ledger.isFreeSpot(level, spot)) {
            fail();
            return;
        }
        int placed = ledger.place(level, spot, piece, yaw, occurrenceKey, villageKey);
        dropCarried(v);
        if (placed > 0) {
            particles(level, Vec3.atBottomCenterOf(spot).add(0, 0.5, 0), ParticleTypes.HAPPY_VILLAGER, 8, 0.5);
            sound(level, v, SoundEvents.WOOD_PLACE, 0.8f, 1.1f);
            sound(level, v, SoundEvents.VILLAGER_YES, 0.6f, 1f);
            onPlaced.accept(placed);
            finish();
        } else {
            fail();
        }
    }
}
