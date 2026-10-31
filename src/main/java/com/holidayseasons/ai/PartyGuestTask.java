package com.holidayseasons.ai;

import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A villager invited to a player's party walks to the home's entrance on its own feet (vanilla pathfinding; if it
 * cannot get there it gives up), then mingles: dances, hums, snacks and cheers until the party ends.
 */
public final class PartyGuestTask extends CelebrationTask {
    private final UUID homeId;
    private final BlockPos spot;
    private final long endsAt;
    private final int maxTicks;
    private final ItemStack food;
    private boolean arrived;
    private int partying;

    public PartyGuestTask(String occ, String village, UUID homeId, BlockPos spot, long now, long endsAt, ItemStack food) {
        super(TaskType.PARTY_GUEST, occ, village);
        this.homeId = homeId;
        this.spot = spot;
        this.endsAt = endsAt;
        this.maxTicks = (int) Math.min(30000, Math.max(200, endsAt - now + 1200));
        this.food = food;
    }

    public boolean arrived() {
        return arrived;
    }

    public UUID homeId() {
        return homeId;
    }

    @Override
    protected int maxTicks() {
        return maxTicks;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (level.getGameTime() >= endsAt) {
            finish();
            return;
        }
        if (!arrived) {
            if (!walkTo(v, spot, 0.55f, 2)) return;
            arrived = true;
            EverFestServer s = EverFestServer.get();
            if (s != null) s.parties().guestArrived(homeId, v);
            sound(level, v, SoundEvents.VILLAGER_CELEBRATE, 0.8f, 1f);
        }
        partying++;
        lookAt(v, spot);
        if (partying % 40 == 0) {
            hop(v);
            particles(level, head(v), ParticleTypes.NOTE, 1, 0.2);
        }
        if (partying % 120 == 60) particles(level, head(v), ParticleTypes.HEART, 2, 0.3);
        if (partying % 200 == 100 && !food.isEmpty()) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food), v.getX(), v.getEyeY() - 0.2, v.getZ(), 6, 0.15, 0.1, 0.15, 0.03);
            sound(level, v, SoundEvents.GENERIC_EAT, 0.6f, 1f);
        }
        if (partying % 260 == 130) sound(level, v, SoundEvents.VILLAGER_YES, 0.6f, 0.9f + level.random.nextFloat() * 0.3f);
    }
}
