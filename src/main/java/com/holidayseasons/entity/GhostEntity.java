package com.holidayseasons.entity;

import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.sound.EverFestSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A mischievous Halloween ghost. Drifts toward a nearby player, swipes one piece of candy (or gives a tiny nip if
 * there is none), then flees. It avoids light, gives up quickly and never pursues relentlessly.
 */
public class GhostEntity extends Monster {
    private int fleeTicks;
    private int chaseTicks;

    public GhostEntity(EntityType<? extends GhostEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FleeGoal());
        goalSelector.addGoal(2, new SpookGoal());
        goalSelector.addGoal(5, new DriftGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.5), getY() + random.nextDouble() * 0.6, getRandomZ(0.5), 0, -0.02, 0);
            }
            return;
        }
        if (fleeTicks > 0) fleeTicks--;
        // Light makes ghosts uneasy: bright places send them away.
        if (level().getBrightness(LightLayer.BLOCK, blockPosition()) >= 11 && fleeTicks == 0) fleeTicks = 80;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return EverFestSounds.GHOST_MOAN.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return EverFestSounds.GHOST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return EverFestSounds.GHOST_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    /** Floats toward the target, then plays a trick once close. */
    class SpookGoal extends Goal {
        @Nullable
        private Player target;

        SpookGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (fleeTicks > 0) return false;
            target = level().getNearestPlayer(GhostEntity.this, 14);
            return target != null && !target.isCreative() && !target.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && fleeTicks == 0 && chaseTicks < 600 && distanceToSqr(target) < 24 * 24;
        }

        @Override
        public void stop() {
            chaseTicks = 0;
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) return;
            chaseTicks++;
            getLookControl().setLookAt(target, 30, 30);
            getMoveControl().setWantedPosition(target.getX(), target.getEyeY() - 0.4, target.getZ(), 1.0);
            if (distanceToSqr(target) < 2.2) {
                trick(target);
                fleeTicks = 200;
            }
        }
    }

    private void trick(Player player) {
        if (!(level() instanceof ServerLevel server)) return;
        boolean stole = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(EverFestItems.CANDY_TAG)) {
                s.shrink(1);
                stole = true;
                break;
            }
        }
        if (stole) {
            if (player instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("holidayseasons.ghost.stole_candy"), true);
        } else {
            doHurtTarget(player);
        }
        playSound(EverFestSounds.WHISPERS.get(), 0.8f, 1.2f);
        server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
    }

    /** Swoops away from players after a trick or when in bright light. */
    class FleeGoal extends Goal {
        FleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeTicks > 0;
        }

        @Override
        public void tick() {
            Player p = level().getNearestPlayer(GhostEntity.this, 24);
            Vec3 away = p == null ? new Vec3(random.nextGaussian(), 0.3, random.nextGaussian())
                    : position().subtract(p.position()).normalize();
            Vec3 dest = position().add(away.scale(6)).add(0, 1.5, 0);
            getMoveControl().setWantedPosition(dest.x, Math.min(dest.y, getY() + 2), dest.z, 1.3);
        }
    }

    /** Idle drifting a few blocks around, staying low to the ground. */
    class DriftGoal extends Goal {
        DriftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(20) == 0;
        }

        @Override
        public void start() {
            BlockPos ground = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, blockPosition());
            double y = Math.min(getY() + random.nextGaussian(), ground.getY() + 3.5);
            getMoveControl().setWantedPosition(getX() + random.nextGaussian() * 5, Math.max(ground.getY() + 1.0, y), getZ() + random.nextGaussian() * 5, 0.6);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    public static boolean isSpookyMob(Mob mob) {
        return mob instanceof GhostEntity || mob instanceof GourdlingEntity || mob instanceof ScarecrowEntity;
    }
}
