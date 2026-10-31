package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.calendar.Season;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A pastel spring bunny. It hops like a rabbit (it is one), appears only in spring, and while spring or Easter lasts it
 * now and then leaves a painted egg behind, like a chicken lays eggs. Wild bunnies quietly go back to their burrows
 * once spring is over (they despawn when no player is near); bred, named or leashed bunnies stay.
 */
public class SpringBunnyEntity extends Rabbit {
    public static final int COATS = 4;
    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(SpringBunnyEntity.class, EntityDataSerializers.INT);
    private int eggTime;
    private boolean homebred;

    public SpringBunnyEntity(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
        this.eggTime = 6000 + random.nextInt(6000);
    }

    public static boolean checkSpringBunnySpawnRules(EntityType<SpringBunnyEntity> type, LevelAccessor level, MobSpawnType spawnType,
                                                     BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COAT, 0);
    }

    public int coat() {
        return entityData.get(COAT);
    }

    public void setCoat(int coat) {
        entityData.set(COAT, Math.floorMod(coat, COATS));
    }

    public boolean homebred() {
        return homebred;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Coat", coat());
        tag.putInt("EggLayTime", eggTime);
        tag.putBoolean("Homebred", homebred);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCoat(tag.getInt("Coat"));
        if (tag.contains("EggLayTime")) eggTime = tag.getInt("EggLayTime");
        homebred = tag.getBoolean("Homebred");
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        setCoat(random.nextInt(COATS));
        return data;
    }

    @Nullable
    @Override
    public SpringBunnyEntity getBreedOffspring(ServerLevel level, AgeableMob other) {
        SpringBunnyEntity kit = EverFestEntities.SPRING_BUNNY.get().create(level);
        if (kit != null) {
            kit.setCoat(random.nextBoolean() || !(other instanceof SpringBunnyEntity b) ? coat() : b.coat());
            kit.homebred = true;
        }
        return kit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || isBaby()) return;
        if (--eggTime <= 0) {
            eggTime = 6000 + random.nextInt(6000);
            if (springtime()) {
                Item[] eggs = {EverFestItems.EASTER_EGG_PINK.get(), EverFestItems.EASTER_EGG_BLUE.get(),
                        EverFestItems.EASTER_EGG_YELLOW.get(), EverFestItems.EASTER_EGG_GREEN.get()};
                spawnAtLocation(eggs[random.nextInt(eggs.length)]);
                playSound(SoundEvents.CHICKEN_EGG, 0.8f, 1.4f + random.nextFloat() * 0.2f);
            }
        }
    }

    /** Lays eggs immediately on the next tick (tests and operators). */
    public void layEggSoon() {
        eggTime = 1;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !homebred && !isLeashed() && !springtime();
    }

    /** Spring (calendar season), or Easter preparing/celebrating. */
    public static boolean springtime() {
        EverFestServer s = EverFestServer.get();
        if (s == null) return true;
        var f = s.director().festival();
        if (f != null && f.holiday().id().equals(EverFest.MOD_ID + ":easter")
                && (f.phase() == HolidayPhase.PREPARING || f.phase() == HolidayPhase.ACTIVE)) return true;
        return s.calendar().snapshot().season() == Season.SPRING;
    }
}
