package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An autumn wild turkey. Flocks wander woods and meadows, toms strut with their tail fanned and gobble, and the whole
 * flock keeps away from players unless they sneak or carry seeds. Turkeys flutter down gently like chickens. Wild
 * turkeys leave again when autumn is over (they despawn when no player is near); bred, named or leashed ones stay.
 */
public class WildTurkeyEntity extends Animal {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, EverFest.id("turkey_food"));
    private static final EntityDataAccessor<Boolean> STRUTTING = SynchedEntityData.defineId(WildTurkeyEntity.class, EntityDataSerializers.BOOLEAN);
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1f;
    public float fan;
    public float oFan;
    private boolean homebred;

    public WildTurkeyEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static boolean checkTurkeySpawnRules(EntityType<WildTurkeyEntity> type, LevelAccessor level, MobSpawnType spawnType,
                                                BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STRUTTING, false);
    }

    public boolean strutting() {
        return entityData.get(STRUTTING);
    }

    void setStrutting(boolean strutting) {
        entityData.set(STRUTTING, strutting);
    }

    public boolean homebred() {
        return homebred;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Homebred", homebred);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        homebred = tag.getBoolean("Homebred");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> s.is(FOOD), false));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 7f, 1.1, 1.4,
                e -> e instanceof Player p && !p.isShiftKeyDown() && !p.isCreative() && !p.isSpectator() && !isFood(p.getMainHandItem())));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new StrutGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public WildTurkeyEntity getBreedOffspring(ServerLevel level, AgeableMob other) {
        WildTurkeyEntity poult = EverFestEntities.WILD_TURKEY.get().create(level);
        if (poult != null) poult.homebred = true;
        return poult;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Chicken-style fluttering.
        oFlap = flap;
        oFlapSpeed = flapSpeed;
        flapSpeed += (onGround() ? -1f : 4f) * 0.3f;
        flapSpeed = Mth.clamp(flapSpeed, 0f, 1f);
        if (!onGround() && flapping < 1f) flapping = 1f;
        flapping *= 0.9f;
        Vec3 v = getDeltaMovement();
        if (!onGround() && v.y < 0) setDeltaMovement(v.multiply(1, 0.6, 1));
        flap += flapping * 2f;
        oFan = fan;
        fan = Mth.clamp(fan + (strutting() ? 0.15f : -0.15f), 0f, 1f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !homebred && !isLeashed() && !autumnTime();
    }

    /** Autumn (calendar season), or Thanksgiving preparing/celebrating. */
    public static boolean autumnTime() {
        EverFestServer s = EverFestServer.get();
        if (s == null) return true;
        var f = s.director().festival();
        if (f != null && f.holiday().id().equals(EverFest.MOD_ID + ":thanksgiving")
                && (f.phase() == HolidayPhase.PREPARING || f.phase() == HolidayPhase.ACTIVE)) return true;
        return s.calendar().snapshot().season() == Season.AUTUMN;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return strutting() ? EverFestSounds.TURKEY_GOBBLE.get() : SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return strutting() ? 1f : super.getVoicePitch() * 0.6f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHICKEN_STEP, 0.15f, 0.8f);
    }

    /** A tom stops, fans its tail and gobbles for a few seconds. */
    static final class StrutGoal extends Goal {
        private final WildTurkeyEntity self;
        private int time;

        StrutGoal(WildTurkeyEntity self) {
            this.self = self;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !self.isBaby() && self.onGround() && !self.isInWater() && self.getRandom().nextInt(500) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return time > 0 && self.getLastHurtByMob() == null;
        }

        @Override
        public void start() {
            time = adjustedTickDelay(60 + self.getRandom().nextInt(60));
            self.getNavigation().stop();
            self.setStrutting(true);
            self.playSound(EverFestSounds.TURKEY_GOBBLE.get(), 1f, 0.9f + self.getRandom().nextFloat() * 0.2f);
        }

        @Override
        public void tick() {
            time--;
            if (time == 30) self.playSound(EverFestSounds.TURKEY_GOBBLE.get(), 1f, 0.95f);
        }

        @Override
        public void stop() {
            self.setStrutting(false);
        }
    }
}
