package com.holidayseasons.ai;

import com.holidayseasons.event.EasterService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

/** A child searches for hidden eggs, runs to one, picks it up and cheers, then looks for the next (a few times). */
public final class EggHuntTask extends CelebrationTask {
    private final EasterService easter;
    private Interaction target;
    private int found;
    private int cheering = -1;

    public EggHuntTask(String occ, String village, EasterService easter, Interaction first) {
        super(TaskType.EGG_HUNT, occ, village);
        this.easter = easter;
        this.target = first;
    }

    @Override
    protected int maxTicks() {
        return 3600;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (cheering >= 0) {
            if (++cheering % 10 == 0) hop(v);
            if (cheering < 40) return;
            dropCarried(v);
            cheering = -1;
            if (found >= 3) {
                finish();
                return;
            }
            target = easter.nearestEgg(level, v.blockPosition(), 24);
        }
        if (target == null || target.isRemoved()) {
            target = easter.nearestEgg(level, v.blockPosition(), 24);
            if (target == null) {
                finish();
                return;
            }
        }
        if (!walkTo(v, target.blockPosition(), 0.6f, 1)) {
            if (failed()) {
                clearFailure(v);
                target = null;
            }
            return;
        }
        ItemStack egg = easter.collect(v, target);
        target = null;
        if (!egg.isEmpty()) {
            found++;
            carry(v, egg);
            sound(level, v, SoundEvents.VILLAGER_CELEBRATE, 0.8f, 1.6f);
            cheering = 0;
        }
    }
}
