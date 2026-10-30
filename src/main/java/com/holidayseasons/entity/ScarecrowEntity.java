package com.holidayseasons.entity;

import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

/**
 * A harmless scarecrow that only moves when nobody is looking: it turns to face the nearest player and now and then
 * shuffles a little closer (never nearer than a few blocks). Look at it and it freezes. It never attacks.
 */
public class ScarecrowEntity extends PathfinderMob {
    private int shuffleCooldown = 60;

    public ScarecrowEntity(EntityType<? extends ScarecrowEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** True if {@code player} is looking roughly at this scarecrow. */
    public boolean isWatchedBy(Player player) {
        Vec3 look = player.getViewVector(1f).normalize();
        Vec3 to = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double dist = to.length();
        if (dist < 1e-3) return true;
        return look.dot(to.scale(1 / dist)) > 0.55 && player.hasLineOfSight(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 10 != 0) return;
        Player nearest = level().getNearestPlayer(this, 24);
        if (nearest == null || nearest.isSpectator()) return;
        for (Player p : level().players()) {
            if (!p.isSpectator() && p.distanceToSqr(this) < 32 * 32 && isWatchedBy(p)) return;
        }
        // Unobserved: turn to face the player...
        double dx = nearest.getX() - getX();
        double dz = nearest.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180 / Math.PI)) - 90f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        // ...and occasionally shuffle one step closer.
        if (--shuffleCooldown <= 0 && nearest.distanceToSqr(this) > 25) {
            shuffleCooldown = 6 + random.nextInt(10);
            Vec3 step = new Vec3(dx, 0, dz).normalize();
            BlockPos dest = BlockPos.containing(getX() + step.x, getY(), getZ() + step.z);
            if (level().getBlockState(dest).isAir() && level().getBlockState(dest.above()).isAir()
                    && !level().getBlockState(dest.below()).isAir()) {
                moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, yaw, 0);
                playSound(random.nextBoolean() ? EverFestSounds.SCARECROW_RUSTLE.get() : EverFestSounds.SCARECROW_CREAK.get(), 0.5f, 0.8f + random.nextFloat() * 0.3f);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EverFestSounds.SCARECROW_RUSTLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GRASS_BREAK;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(Items.WHEAT, 2 + random.nextInt(3)));
        if (random.nextFloat() < 0.4f) spawnAtLocation(new ItemStack(Items.CARVED_PUMPKIN));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
