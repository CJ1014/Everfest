package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.server.EverFestServer;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * A reindeer-like deer of snowy biomes. Herds stay together behind their oldest member, keep their distance from
 * sprinting players unless tempted with food, and can be bred with wheat or berries. Coats: brown, frosty, and a rare
 * festive red-nosed coat that only appears while it is winter or Christmas is near.
 */
public class ReindeerEntity extends Animal {
    public static final int BROWN = 0;
    public static final int FROSTY = 1;
    public static final int FESTIVE = 2;
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, EverFest.id("reindeer_food"));
    public static final TagKey<net.minecraft.world.level.block.Block> SPAWNABLE_ON =
            TagKey.create(Registries.BLOCK, EverFest.id("reindeer_spawnable_on"));
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ReindeerEntity.class, EntityDataSerializers.INT);

    public ReindeerEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    /** Natural spawning: snowy ground, enough light, and the server config allows reindeer. */
    public static boolean checkReindeerSpawnRules(EntityType<? extends Animal> type, LevelAccessor level, MobSpawnType spawnType,
                                                  BlockPos pos, RandomSource random) {
        if (ServerConfig.SPEC.isLoaded() && !ServerConfig.INSTANCE.reindeer.get()) return false;
        BlockState below = level.getBlockState(pos.below());
        return (below.is(SPAWNABLE_ON) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, BROWN);
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, Math.floorMod(variant, 3));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.15, s -> s.is(FOOD), false));
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 9f, 1.2, 1.7,
                e -> e instanceof Player p && p.isSprinting() && !p.isCreative() && !p.isSpectator() && !isFood(p.getMainHandItem())));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new FollowHerdGoal(this));
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
    public ReindeerEntity getBreedOffspring(ServerLevel level, AgeableMob other) {
        ReindeerEntity calf = EverFestEntities.REINDEER.get().create(level);
        if (calf != null) calf.setVariant(random.nextBoolean() || !(other instanceof ReindeerEntity r) ? variant() : r.variant());
        return calf;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        setVariant(pickVariant(level, blockPosition(), random));
        if (groupData == null) groupData = new AgeableMob.AgeableMobGroupData(0.2f);
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** Frosty coats in cold places; a 1 in 12 festive red nose while it is winter or Christmas is preparing/celebrating. */
    static int pickVariant(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (festiveTime() && random.nextInt(12) == 0) return FESTIVE;
        Biome biome = level.getBiome(pos).value();
        boolean cold = biome.coldEnoughToSnow(pos);
        return cold && random.nextInt(5) < 3 ? FROSTY : BROWN;
    }

    static boolean festiveTime() {
        EverFestServer s = EverFestServer.get();
        if (s == null) return false;
        var f = s.director().festival();
        boolean christmas = f != null && f.holiday().id().equals(EverFest.MOD_ID + ":christmas")
                && (f.phase() == HolidayPhase.PREPARING || f.phase() == HolidayPhase.ACTIVE);
        return christmas || s.calendar().snapshot().season() == Season.WINTER;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Festive reindeer jingle now and then.
        if (!level().isClientSide && variant() == FESTIVE && isAlive() && random.nextInt(400) == 0) {
            playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.4f, 1.6f + random.nextFloat() * 0.3f);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.15f, 1.0f);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75f;
    }

    /**
     * Herd behaviour: each reindeer follows the oldest herd-mate it can see (lowest entity id), so a herd forms a loose
     * line behind one leader instead of every animal circling every other.
     */
    static final class FollowHerdGoal extends Goal {
        private final ReindeerEntity self;
        @Nullable
        private ReindeerEntity leader;
        private int recheck;

        FollowHerdGoal(ReindeerEntity self) {
            this.self = self;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (self.isBaby() || self.isLeashed() || self.random.nextInt(40) != 0) return false;
            leader = findLeader();
            return leader != null && self.distanceToSqr(leader) > 36;
        }

        @Nullable
        private ReindeerEntity findLeader() {
            List<ReindeerEntity> herd = self.level().getEntitiesOfClass(ReindeerEntity.class, self.getBoundingBox().inflate(16, 6, 16),
                    r -> r != self && !r.isBaby() && r.getId() < self.getId());
            ReindeerEntity best = null;
            for (ReindeerEntity r : herd) if (best == null || r.getId() < best.getId()) best = r;
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && self.distanceToSqr(leader) > 9 && self.distanceToSqr(leader) < 24 * 24;
        }

        @Override
        public void start() {
            recheck = 0;
        }

        @Override
        public void stop() {
            leader = null;
            self.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (leader != null && --recheck <= 0) {
                recheck = adjustedTickDelay(20);
                self.getNavigation().moveTo(leader, 1.0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // The herd startles together.
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            for (ReindeerEntity r : level().getEntitiesOfClass(ReindeerEntity.class, getBoundingBox().inflate(10, 4, 10), r -> r != this)) {
                r.setLastHurtByMob(attacker);
            }
        }
        return hurt;
    }
}
