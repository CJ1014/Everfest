package com.holidayseasons.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import java.util.List;
import java.util.function.Consumer;

/**
 * Launch a few fireworks at the festival square. Rockets carry no explosion data (so they can never hurt anyone);
 * the director draws the burst with particles when each rocket expires.
 */
public final class FireworksTask extends CelebrationTask {
    private final BlockPos launchSpot;
    private final Consumer<FireworkRocketEntity> register;
    private int launched;
    private int timer;

    public FireworksTask(String occ, String village, BlockPos launchSpot, Consumer<FireworkRocketEntity> register) {
        super(TaskType.FIREWORKS, occ, village);
        this.launchSpot = launchSpot;
        this.register = register;
    }

    @Override
    protected void onTick(ServerLevel level, Villager v) {
        if (launched == 0 && timer == 0 && !walkTo(v, launchSpot, 0.5f, 2)) return;
        if (timer == 0) carry(v, new ItemStack(Items.FIREWORK_ROCKET));
        if (++timer % 40 != 0) return;
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1 + level.random.nextInt(2), List.of()));
        FireworkRocketEntity e = new FireworkRocketEntity(level, launchSpot.getX() + 0.5 + level.random.nextGaussian() * 0.6,
                launchSpot.getY() + 0.5, launchSpot.getZ() + 0.5 + level.random.nextGaussian() * 0.6, rocket);
        level.addFreshEntity(e);
        register.accept(e);
        if (++launched >= 3) finish();
    }
}
