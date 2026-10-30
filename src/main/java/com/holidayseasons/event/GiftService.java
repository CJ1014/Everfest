package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.home.HomeRecord;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.item.PresentInfo;
import com.holidayseasons.item.PresentItem;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.villager.VillagerPersonality;
import com.holidayseasons.villager.WishList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Christmas gifting, all server-authoritative: wish lists, players giving presents to villagers (one reward per
 * villager per player per Christmas), villagers giving presents to players they like (quality by reputation), and
 * the mysterious overnight gift delivery to registered homes (once per player per Christmas).
 */
public final class GiftService {
    public static final String HOLIDAY_ID = EverFest.MOD_ID + ":christmas";
    private final MinecraftServer server;
    private final EverFestServer services;
    private final Random random = new Random();
    private final Claims claims;

    /** Persistent "already happened" keys so restarts never repeat rewards. */
    public static final class Claims extends SavedData {
        static final String FILE_ID = "holidayseasons_gifts";
        static final Factory<Claims> FACTORY = new Factory<>(Claims::new, (tag, reg) -> {
            Claims c = new Claims();
            for (String k : tag.getAllKeys()) c.keys.add(k);
            return c;
        }, null);
        final Set<String> keys = new HashSet<>();

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            keys.forEach(k -> tag.putBoolean(k, true));
            return tag;
        }

        /** Records {@code key}; returns false if it was already claimed. */
        boolean claim(String key) {
            boolean added = keys.add(key);
            if (added) setDirty();
            return added;
        }

        public boolean has(String key) {
            return keys.contains(key);
        }

        void prune(String keepSuffixContaining) {
            if (keys.removeIf(k -> !k.contains(keepSuffixContaining))) setDirty();
        }
    }

    public GiftService(MinecraftServer server, EverFestServer services) {
        this.server = server;
        this.services = services;
        this.claims = server.overworld().getDataStorage().computeIfAbsent(Claims.FACTORY, Claims.FILE_ID);
    }

    public Claims claims() {
        return claims;
    }

    @Nullable
    public CelebrationDirector.Festival christmas() {
        CelebrationDirector.Festival f = services.director().festival();
        return f != null && f.holiday().id().equals(HOLIDAY_ID) ? f : null;
    }

    public static VillagerPersonality personality(Villager v) {
        String prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()).getPath();
        return VillagerPersonality.of(v.getUUID(), prof, v.isBaby());
    }

    public List<String> wishList(Villager v) {
        int year = services.calendar().schedulingSnapshot().date().year();
        return WishList.of(v.getUUID(), personality(v).giftPreference(), year);
    }

    /** Sneak-click a villager during the Christmas season to hear its wishes. */
    public boolean tellWishes(Player player, Villager v) {
        CelebrationDirector.Festival f = christmas();
        if (f == null || f.phase() == HolidayPhase.CLEANUP) return false;
        List<Component> names = new ArrayList<>();
        for (String id : wishList(v)) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            names.add(new ItemStack(item).getHoverName());
        }
        Component joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined = joined.copy().append(", ");
            joined = joined.copy().append(names.get(i).copy().withStyle(ChatFormatting.GOLD));
        }
        player.displayClientMessage(Component.translatable("holidayseasons.gift.wishes", v.getDisplayName(), joined), false);
        if (v.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + v.getBbHeight() + 0.3, v.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        return true;
    }

    /** A player hands a wrapped present to a villager. Consumes it and returns how much the villager liked it (-1 = refused). */
    public int giveToVillager(Player player, Villager v, ItemStack stack) {
        if (!(stack.getItem() instanceof PresentItem)) return -1;
        PresentInfo info = PresentItem.info(stack);
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<String> ids = new ArrayList<>();
        for (ItemStack s : contents.nonEmptyItemsCopy()) ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
        if (!info.wrapped() || ids.isEmpty()) {
            player.displayClientMessage(Component.translatable("holidayseasons.gift.wrap_first"), true);
            return -1;
        }
        int liked = WishList.appreciation(wishList(v), personality(v).giftPreference(), ids);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        ServerLevel level = (ServerLevel) v.level();
        String occ = christmas() != null ? christmas().occurrence().key() : "any";
        String village = services.director().resolveVillage(v).key();
        long day = services.calendar().snapshot().epochDay();
        boolean firstThisYear = claims.claim("gift|" + occ + "|" + player.getUUID() + "|" + v.getUUID());
        if (firstThisYear) {
            for (int i = 0; i <= liked; i++) services.reputation().add(player.getUUID(), village, ReputationMath.Action.GAVE_PRESENT, day);
        }
        level.sendParticles(liked == 2 ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + v.getBbHeight() + 0.3, v.getZ(),
                liked == 2 ? 6 : 4, 0.4, 0.3, 0.4, 0.02);
        level.playSound(null, v.getX(), v.getY(), v.getZ(), liked > 0 ? SoundEvents.VILLAGER_CELEBRATE : SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL, 1f, v.isBaby() ? 1.6f : 1f);
        player.displayClientMessage(Component.translatable("holidayseasons.gift.reaction." + liked, v.getDisplayName()), true);
        return liked;
    }

    /** Builds a wrapped present for {@code player}; contents scale with the player's reputation in that village. */
    public ItemStack presentFor(String fromName, String toName, String village, UUID playerId, @Nullable Villager giver) {
        int rep = services.reputation().get(playerId, village);
        ReputationMath.Tier tier = ReputationMath.tier(rep);
        List<ItemStack> contents = new ArrayList<>();
        switch (tier) {
            case STRANGER -> contents.add(new ItemStack(random.nextBoolean() ? Items.COOKIE : Items.SWEET_BERRIES, 3 + random.nextInt(4)));
            case NEIGHBOUR -> {
                contents.add(new ItemStack(Items.COOKIE, 6));
                contents.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(2)));
            }
            case FRIEND -> {
                contents.add(new ItemStack(Items.EMERALD, 3 + random.nextInt(3)));
                contents.add(new ItemStack(EverFestItems.CHOCOLATE_BAR.get(), 2));
                contents.add(professionGift(giver));
            }
            case CHERISHED -> {
                contents.add(new ItemStack(Items.EMERALD, 6 + random.nextInt(5)));
                contents.add(professionGift(giver));
                contents.add(new ItemStack(EverFestItems.ORNAMENT.get(), 3));
            }
            case LEGEND -> {
                contents.add(new ItemStack(Items.EMERALD, 10 + random.nextInt(6)));
                contents.add(new ItemStack(Items.DIAMOND, 1 + random.nextInt(2)));
                contents.add(professionGift(giver));
                contents.add(new ItemStack(EverFestItems.SANTA_HAT.get()));
            }
        }
        int[] colors = {0xC0392B, 0x2E8B57, 0x2E6EC0, 0xD4AF37, 0x8E44AD, 0xE8E8E8};
        ItemStack present = new ItemStack(EverFestItems.PRESENT.get());
        present.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        present.set(com.holidayseasons.item.EverFestDataComponents.PRESENT.get(), new PresentInfo(colors[random.nextInt(colors.length)], true, fromName,
                giver == null ? java.util.Optional.empty() : java.util.Optional.of(giver.getUUID()), toName, ""));
        return present;
    }

    private ItemStack professionGift(@Nullable Villager giver) {
        if (giver == null) return new ItemStack(Items.GOLDEN_APPLE);
        String prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(giver.getVillagerData().getProfession()).getPath();
        return switch (prof) {
            case "librarian" -> new ItemStack(Items.BOOK, 3);
            case "farmer" -> new ItemStack(Items.GOLDEN_CARROT, 4);
            case "fisherman" -> new ItemStack(Items.COOKED_SALMON, 6);
            case "cleric" -> new ItemStack(Items.EXPERIENCE_BOTTLE, 4);
            case "armorer", "weaponsmith", "toolsmith" -> new ItemStack(Items.IRON_INGOT, 4);
            case "cartographer" -> new ItemStack(Items.SPYGLASS);
            case "shepherd" -> new ItemStack(Items.WHITE_WOOL, 8);
            case "mason" -> new ItemStack(Items.AMETHYST_SHARD, 4);
            default -> new ItemStack(Items.CAKE);
        };
    }

    /**
     * A villager hands a present to a player it likes. Once per villager per player per Christmas; the present is a
     * targeted item entity only that player can pick up.
     */
    public boolean villagerGivesPresent(Villager v, Player player) {
        CelebrationDirector.Festival f = christmas();
        String occ = f == null ? "any" : f.occurrence().key();
        String village = services.director().resolveVillage(v).key();
        if (!claims.claim("from|" + occ + "|" + v.getUUID() + "|" + player.getUUID())) return false;
        ItemStack present = presentFor(v.getDisplayName().getString(), player.getGameProfile().getName(), village, player.getUUID(), v);
        ItemEntity item = new ItemEntity(v.level(), v.getX(), v.getY() + 0.5, v.getZ(), present);
        item.setTarget(player.getUUID());
        item.setPickUpDelay(10);
        v.level().addFreshEntity(item);
        ((ServerLevel) v.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.5, v.getZ(), 6, 0.4, 0.3, 0.4, 0);
        v.level().playSound(null, v.getX(), v.getY(), v.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1f, 1f);
        player.displayClientMessage(Component.translatable("holidayseasons.gift.from_villager", v.getDisplayName()), false);
        return true;
    }

    /** Players this villager would like to give a present to: nearby, reputation at least NEIGHBOUR, not yet given. */
    @Nullable
    public ServerPlayer giftRecipient(Villager v) {
        CelebrationDirector.Festival f = christmas();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return null;
        String village = services.director().resolveVillage(v).key();
        for (ServerPlayer p : ((ServerLevel) v.level()).players()) {
            if (p.isSpectator() || p.distanceToSqr(v) > 24 * 24) continue;
            if (services.reputation().get(p.getUUID(), village) < ReputationMath.Tier.NEIGHBOUR.threshold) continue;
            if (claims.has("from|" + f.occurrence().key() + "|" + v.getUUID() + "|" + p.getUUID())) continue;
            return p;
        }
        return null;
    }

    /** Christmas night: a mysterious present appears at each online player's home hearth, once per Christmas. */
    public void tick() {
        ServerLevel level = server.overworld();
        if (level.getGameTime() % 100 != 0) return;
        CelebrationDirector.Festival f = christmas();
        if (f == null || f.phase() != HolidayPhase.ACTIVE) return;
        long t = level.getDayTime() % 24000L;
        if (t < 17000 || t > 22000) return;
        double chance = f.holiday().definition().specialEvents().stream()
                .filter(e -> e.id().endsWith("mysterious_gift_delivery")).mapToDouble(e -> e.dailyChance()).findFirst().orElse(0);
        if (chance <= 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<HomeRecord> homes = services.homes().ownedBy(player.getUUID());
            if (homes.isEmpty()) continue;
            String key = "delivery|" + f.occurrence().key() + "|" + player.getUUID();
            if (claims.has(key)) continue;
            // The roll is made once per player per Christmas, deterministically, so restarts don't re-roll.
            if (new Random(key.hashCode()).nextDouble() >= chance) {
                claims.claim(key);
                continue;
            }
            HomeRecord home = homes.get(0);
            ServerLevel homeLevel = server.getLevel(home.dimension);
            if (homeLevel == null || !homeLevel.isLoaded(home.hearth)) continue;
            claims.claim(key);
            deliver(homeLevel, home.hearth.above(), player);
        }
    }

    public void deliver(ServerLevel level, BlockPos at, ServerPlayer player) {
        ItemStack present = presentFor("???", player.getGameProfile().getName(), "mysterious", player.getUUID(), null);
        ItemStack bonus = new ItemStack(EverFestItems.ORNAMENT.get(), 2);
        List<ItemStack> contents = new ArrayList<>();
        present.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemsCopy().forEach(contents::add);
        contents.add(bonus);
        present.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, present);
        item.setTarget(player.getUUID());
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30, 0.6, 1.2, 0.6, 0.02);
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 2f, 1.2f);
        player.displayClientMessage(Component.translatable("holidayseasons.gift.mysterious").withStyle(ChatFormatting.AQUA), false);
    }
}
