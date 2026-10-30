package com.holidayseasons.ai;

import com.holidayseasons.event.EasterService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** An adult sneaks an egg into a hiding spot before Easter. */
public final class HideEggsTask extends CelebrationTask {
    private final BlockPos spot;
    private final EasterService easter;
    private int hiding;

    public HideEggsTask(String occ, String village, BlockPos spot, EasterService easter) {
        super(TaskType.HIDE_EGGS, occ, village);
        this.spot = spot;
        this.easter = easter;
    }

    @Override
    protected void onStart(ServerLevel level, Villager v) {
        carry(v, new ItemStack(Items.EGG));
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (!walkTo(v, spot, 0.5f, 1)) return;
        lookAt(v, spot);
        if (++hiding < 20) return;
        dropCarried(v);
        if (easter.hideEgg(level, spot, occurrenceKey, villageKey)) {
            particles(level, Vec3.atBottomCenterOf(spot).add(0, 0.3, 0), ParticleTypes.HAPPY_VILLAGER, 3, 0.2);
            finish();
        } else {
            fail();
        }
    }
}
