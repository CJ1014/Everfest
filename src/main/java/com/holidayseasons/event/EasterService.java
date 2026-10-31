package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.decoration.DecorationPiece;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Easter egg hunts: adults hide eggs around the village while preparing; on Easter children (and players) find them.
 * An egg is a small block display plus a vanilla interaction hitbox, both tracked by the decoration ledger, so hiding
 * an egg never touches a block and leftovers vanish during cleanup. Each egg can be found exactly once.
 */
public final class EasterService {
    public static final String HOLIDAY_ID = EverFest.MOD_ID + ":easter";
    public static final String EGG_TAG = "holidayseasons_egg";
    public static final String PIECE_ID = "hidden_egg";
    private static final Block[] SHELLS = {Blocks.PINK_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA};
    private final EverFestServer services;
    private final Random random = new Random();

    public EasterService(EverFestServer services) {
        this.services = services;
    }

    @Nullable
    public CelebrationDirector.Festival easter() {
        CelebrationDirector.Festival f = services.director().festival();
        return f != null && f.holiday().id().equals(HOLIDAY_ID) ? f : null;
    }

    public int eggsPerVillage(CelebrationDirector.Festival f) {
        return (int) f.holiday().definition().setting("eggs_per_village", 12);
    }

    /** Hides one egg at {@code pos}. 1 in 20 is golden. */
    public boolean hideEgg(ServerLevel level, BlockPos pos, String occ, String village) {
        boolean golden = random.nextInt(20) == 0;
        int colour = random.nextInt(SHELLS.length);
        Block shell = golden ? Blocks.GOLD_BLOCK : SHELLS[colour];
        DecorationPiece egg = new DecorationPiece(PIECE_ID, List.of(DecorationPiece.Part.box(shell.defaultBlockState(), 0, 0, 0, 0.22f, 0.3f, 0.22f)),
                0, DecorationPiece.Placement.GROUND, ItemStack.EMPTY);
        if (services.ledger().place(level, pos, egg, 0, occ, village) == 0) return false;
        Interaction hitbox = EntityType.INTERACTION.create(level);
        if (hitbox == null) return true;
        CompoundTag tag = hitbox.saveWithoutId(new CompoundTag());
        tag.putFloat("width", 0.5f);
        tag.putFloat("height", 0.45f);
        tag.putBoolean("response", true);
        hitbox.load(tag);
        hitbox.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        hitbox.addTag(EGG_TAG);
        CompoundTag egg2 = new CompoundTag();
        egg2.putInt("colour", golden ? -1 : colour);
        hitbox.getPersistentData().put(EGG_TAG, egg2);
        services.ledger().placeEntity(level, pos, hitbox, occ, village, PIECE_ID);
        return true;
    }

    public static boolean isEgg(net.minecraft.world.entity.Entity e) {
        return e instanceof Interaction && e.getTags().contains(EGG_TAG);
    }

    public static ItemStack eggItem(int colour) {
        Item item = switch (colour) {
            case -1 -> EverFestItems.GOLDEN_EGG.get();
            case 0 -> EverFestItems.EASTER_EGG_PINK.get();
            case 1 -> EverFestItems.EASTER_EGG_BLUE.get();
            case 2 -> EverFestItems.EASTER_EGG_YELLOW.get();
            default -> EverFestItems.EASTER_EGG_GREEN.get();
        };
        return new ItemStack(item);
    }

    /**
     * Someone found an egg. Removes it (display and hitbox) through the ledger first, so it can only be found once,
     * then rewards the finder. Returns the egg item (already given to a player), or empty if it was already gone.
     */
    public ItemStack collect(LivingEntity finder, Interaction hitbox) {
        if (!isEgg(hitbox) || hitbox.isRemoved() || !(hitbox.level() instanceof ServerLevel level)) return ItemStack.EMPTY;
        int colour = hitbox.getPersistentData().getCompound(EGG_TAG).getInt("colour");
        String occ = hitbox.getPersistentData().getCompound(com.holidayseasons.decoration.DecorationLedger.ENTITY_TAG).getString("occ");
        BlockPos pos = hitbox.blockPosition();
        int removed = services.ledger().removeDecorationAt(level, pos, occ);
        if (!hitbox.isRemoved()) hitbox.discard();
        if (removed == 0) return ItemStack.EMPTY;
        ItemStack egg = eggItem(colour);
        level.sendParticles(colour == -1 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                colour == -1 ? 20 : 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, pos, colour == -1 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1f, 1.3f);
        if (finder instanceof Player player) {
            if (!player.addItem(egg.copy())) player.drop(egg.copy(), false);
            String village = nearestVillage(level, pos);
            if (village != null) {
                services.reputation().add(player.getUUID(), village, ReputationMath.Action.FESTIVAL_PARTICIPATION, services.calendar().snapshot().epochDay());
            }
            if (colour == -1) player.displayClientMessage(Component.translatable("holidayseasons.easter.golden"), false);
            com.holidayseasons.economy.HolidayTokens.award(player, 1, "holidayseasons.tokens.reason.egg");
        }
        return egg;
    }

    @Nullable
    private String nearestVillage(ServerLevel level, BlockPos pos) {
        return services.director().villages().all().stream()
                .filter(r -> r.key.startsWith(level.dimension().location() + "|bell|") && r.centre().distSqr(pos) < 96 * 96)
                .min(Comparator.comparingDouble(r -> r.centre().distSqr(pos))).map(r -> r.key).orElse(null);
    }

    /** The nearest unfound egg within {@code radius} of {@code near}, for children hunting. */
    @Nullable
    public Interaction nearestEgg(ServerLevel level, BlockPos near, int radius) {
        return level.getEntitiesOfClass(Interaction.class, new AABB(near).inflate(radius), EasterService::isEgg).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(near.getCenter()))).orElse(null);
    }
}
