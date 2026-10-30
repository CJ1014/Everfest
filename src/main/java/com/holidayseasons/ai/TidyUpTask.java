package com.holidayseasons.ai;

import com.holidayseasons.decoration.DecorationLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Walk to a decoration scheduled to come down and take it away. */
public final class TidyUpTask extends CelebrationTask {
    private final BlockPos pos;
    private final String decorationOccurrence;
    private final DecorationLedger ledger;
    private final ItemStack carriedAway;
    private int afterTicks = -1;

    public TidyUpTask(String occ, String village, BlockPos pos, String decorationOccurrence, DecorationLedger ledger, ItemStack carriedAway) {
        super(TaskType.TIDY_UP, occ, village);
        this.pos = pos;
        this.decorationOccurrence = decorationOccurrence;
        this.ledger = ledger;
        this.carriedAway = carriedAway;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (afterTicks >= 0) {
            if (++afterTicks > 60) finish();
            return;
        }
        if (!walkTo(v, pos, 0.5f, 2)) return;
        lookAt(v, pos);
        int removed = ledger.removeDecorationAt(level, pos, decorationOccurrence);
        if (removed > 0) {
            particles(level, Vec3.atBottomCenterOf(pos).add(0, 0.5, 0), ParticleTypes.POOF, 6, 0.3);
            sound(level, v, SoundEvents.WOOD_BREAK, 0.6f, 1.2f);
            carry(v, carriedAway);
        }
        afterTicks = 0;
    }
}
