package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.economy.MerchantTrades;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * A travelling holiday merchant. Holiday merchants visit a celebrating village for a while and leave when their
 * holiday ends; the Midnight Peddler appears on Halloween night and vanishes at dawn. Trades come from
 * {@link MerchantTrades}: a handful of limited-use offers that never restock, so nothing can be farmed. Unlike a
 * wandering trader, merchants never drink invisibility potions at night.
 */
public class HolidayMerchantEntity extends WanderingTrader {
    public static final String MIDNIGHT = "midnight";

    public enum Look {
        WINTER, SPOOKY, MIDNIGHT, SPRING, HARVEST, CELEBRATION;

        public String texture() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(HolidayMerchantEntity.class, EntityDataSerializers.INT);
    /** The holiday this merchant came for ({@code namespace:id}), or {@link #MIDNIGHT}. */
    private String occasion = "";

    public HolidayMerchantEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    public static Look lookFor(String occasion) {
        if (occasion.equals(MIDNIGHT)) return Look.MIDNIGHT;
        String path = occasion.substring(occasion.indexOf(':') + 1);
        return switch (path) {
            case "christmas" -> Look.WINTER;
            case "halloween" -> Look.SPOOKY;
            case "easter", "valentines" -> Look.SPRING;
            case "thanksgiving" -> Look.HARVEST;
            default -> Look.CELEBRATION;
        };
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
        entityData.set(LOOK, lookFor(occasion).ordinal());
        dressUp();
    }

    public String occasion() {
        return occasion;
    }

    public Look look() {
        int i = entityData.get(LOOK);
        return Look.values()[Math.floorMod(i, Look.values().length)];
    }

    /** A hat for the occasion, worn through the vanilla head layer. Never dropped. */
    private void dressUp() {
        ItemStack hat = switch (lookFor(occasion)) {
            case WINTER -> new ItemStack(EverFestItems.SANTA_HAT.get());
            case SPOOKY -> new ItemStack(EverFestItems.COSTUMES.get(com.holidayseasons.villager.VillagerPersonality.Costume.WITCH).get());
            case SPRING -> new ItemStack(EverFestItems.WINTER_BEANIE.get());
            default -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.HEAD, hat);
        setDropChance(EquipmentSlot.HEAD, 0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOK, Look.CELEBRATION.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Occasion", occasion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        occasion = tag.getString("Occasion");
        entityData.set(LOOK, lookFor(occasion).ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Evoker.class, 12f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vex.class, 8f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15f, 0.5, 0.5));
        goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3f, 1f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, net.minecraft.world.entity.Mob.class, 8f));
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.addAll(MerchantTrades.offersFor(occasion, com.holidayseasons.economy.HolidayTokens.enabled(), random));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (look() == Look.MIDNIGHT && random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + random.nextDouble() * 0.4, getRandomZ(0.6), 0, 0.01, 0);
            }
            return;
        }
        if (tickCount % 100 == 0 && !isTrading() && timeToLeave()) leave();
    }

    /** Holiday merchants leave when their holiday stops celebrating; the Midnight Peddler at dawn. */
    public boolean timeToLeave() {
        if (occasion.isEmpty()) return false;
        long tod = level().getDayTime() % 24000L;
        if (occasion.equals(MIDNIGHT)) return tod < 13000 || tod >= 23000;
        EverFestServer s = EverFestServer.get();
        if (s == null) return false;
        var f = s.director().festival();
        return f == null || !f.holiday().id().equals(occasion) || f.phase() != HolidayPhase.ACTIVE;
    }

    /** Leaves in a puff of smoke (nothing is dropped). */
    public void leave() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(look() == Look.MIDNIGHT ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                    getX(), getY() + 1, getZ(), 24, 0.4, 0.8, 0.4, 0.02);
            playSound(SoundEvents.WANDERING_TRADER_DISAPPEARED, 1f, look() == Look.MIDNIGHT ? 0.6f : 1f);
        }
        EverFest.LOGGER.debug("EverFest: {} merchant left at {}", occasion, blockPosition());
        discard();
    }

    /** Merchants stay near the village square (or where the peddler appeared). */
    public void settleAround(BlockPos centre, int radius) {
        restrictTo(centre, radius);
        setWanderTarget(centre);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
