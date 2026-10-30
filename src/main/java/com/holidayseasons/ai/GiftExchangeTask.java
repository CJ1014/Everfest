package com.holidayseasons.ai;

import com.holidayseasons.event.GiftService;
import com.holidayseasons.item.EverFestItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Carry a present to someone: another villager (who unwraps it on the spot - a visual exchange), or a player the
 * villager likes (who receives a real present through {@link GiftService}).
 */
public final class GiftExchangeTask extends CelebrationTask {
    private final LivingEntity recipient;
    @Nullable
    private final GiftService gifts;
    private int handing = -1;

    public GiftExchangeTask(String occ, String village, LivingEntity recipient, @Nullable GiftService gifts) {
        super(TaskType.GIFT_EXCHANGE, occ, village);
        this.recipient = recipient;
        this.gifts = gifts;
    }

    @Override
    protected void onStart(ServerLevel level, Villager v) {
        // Valentine's gifts are flowers; other holidays bring a wrapped present.
        carry(v, occurrenceKey.contains(":valentines|") ? new ItemStack(net.minecraft.world.item.Items.POPPY) : new ItemStack(EverFestItems.PRESENT.get()));
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (!recipient.isAlive() || recipient.level() != level) {
            finish();
            return;
        }
        if (handing < 0) {
            if (!walkTo(v, recipient, 0.55f, 2)) return;
            handing = 0;
        }
        lookAt(v, recipient);
        if (recipient instanceof Villager other) lookAt(other, v);
        handing++;
        if (handing == 15) {
            dropCarried(v);
            if (recipient instanceof ServerPlayer player && gifts != null && !occurrenceKey.contains(":valentines|")) {
                gifts.villagerGivesPresent(v, player);
            } else if (recipient instanceof Villager other) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, other.getX(), other.getY() + other.getBbHeight() + 0.3, other.getZ(), 6, 0.4, 0.3, 0.4, 0);
                level.playSound(null, other.getX(), other.getY(), other.getZ(), SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1f);
                sound(level, other, SoundEvents.VILLAGER_CELEBRATE, 0.8f, other.isBaby() ? 1.6f : 1f);
            }
        }
        if (handing == 25) particles(level, head(v), ParticleTypes.HEART, 2, 0.3);
        if (handing >= 40) finish();
    }
}
