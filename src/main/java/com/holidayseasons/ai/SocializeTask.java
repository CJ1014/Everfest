package com.holidayseasons.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;

/** Walk over to another villager and chat for a while, both facing each other. */
public final class SocializeTask extends CelebrationTask {
    private final Villager partner;
    private int chatted;

    public SocializeTask(String occ, String village, Villager partner) {
        super(TaskType.SOCIALIZE, occ, village);
        this.partner = partner;
    }

    @Override
    protected int maxTicks() {
        return 900;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (!partner.isAlive() || partner.isRemoved() || partner.level() != level || partner.isSleeping()) {
            finish();
            return;
        }
        if (chatted == 0 && !walkTo(v, partner, 0.5f, 2)) return;
        if (v.distanceToSqr(partner) > 25) {
            chatted = 0;
            return;
        }
        chatted++;
        lookAt(v, partner);
        lookAt(partner, v);
        if (chatted % 35 == 0) {
            particles(level, head(v), chatted % 70 == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.NOTE, 1, 0.15);
            sound(level, chatted % 70 == 0 ? partner : v, SoundEvents.VILLAGER_AMBIENT, 0.5f, 0.9f + level.random.nextFloat() * 0.3f);
        }
        if (chatted >= 160) {
            particles(level, head(v).add(head(partner)).scale(0.5), ParticleTypes.HEART, 2, 0.3);
            finish();
        }
    }
}
