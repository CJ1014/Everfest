package com.holidayseasons.entity;

import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** A little hopping pumpkin. Weak and a bit silly; bursts into seeds (and sometimes candy) when defeated. */
public class GourdlingEntity extends Monster {
    private int hopCooldown;
    /** Client-side squash animation progress. */
    public float squish;
    public float squishO;

    public GourdlingEntity(EntityType<? extends GourdlingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        squishO = squish;
        if (level().isClientSide) {
            squish *= 0.6f;
            if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.0004 && random.nextInt(8) == 0) squish = 1f;
            return;
        }
        if (hopCooldown > 0) hopCooldown--;
        if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.0004 && hopCooldown == 0) {
            jumpFromGround();
            hopCooldown = 10 + random.nextInt(8);
            playSound(EverFestSounds.GOURDLING_HOP.get(), 0.5f, 0.9f + random.nextFloat() * 0.3f);
        }
    }

    @Override
    protected float getJumpPower() {
        return 0.32f;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PUMPKIN.defaultBlockState()),
                    getX(), getY() + 0.4, getZ(), 24, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(Items.PUMPKIN_SEEDS, 1 + random.nextInt(2)));
        if (recentlyHit && random.nextFloat() < 0.15f) spawnAtLocation(new ItemStack(EverFestItems.CANDY_CORN.get()));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EverFestSounds.GOURDLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return EverFestSounds.GOURDLING_DEATH.get();
    }
}
