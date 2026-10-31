package com.holidayseasons.event;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.economy.HolidayTokens;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.reputation.ReputationMath;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.village.VillageRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Village-wide holiday objectives. While a holiday is preparing or celebrating, each loaded bell village asks for a
 * batch of something festive (see {@link ObjectiveRules}). Players bring it by using the items on the village bell;
 * a boss bar near the bell shows progress. When it is complete every contributor earns reputation in that village
 * (and festival tokens, if enabled), once per village per holiday, and the village celebrates with fireworks.
 */
public final class VillageObjectives {
    private static final int BAR_RANGE = 32;
    private final EverFestServer services;
    private final Data data;
    private final Map<String, ServerBossEvent> bars = new HashMap<>();

    public VillageObjectives(EverFestServer services) {
        this.services = services;
        this.data = services.server().overworld().getDataStorage().computeIfAbsent(Data.FACTORY, Data.FILE_ID);
    }

    public static boolean enabled() {
        return !ServerConfig.SPEC.isLoaded() || ServerConfig.INSTANCE.villageObjectives.get();
    }

    @Nullable
    private CelebrationDirector.Festival festival() {
        CelebrationDirector.Festival f = services.director().festival();
        return f != null && (f.phase() == HolidayPhase.PREPARING || f.phase() == HolidayPhase.ACTIVE) ? f : null;
    }

    private static String key(String occ, String village) {
        return occ + "|" + village;
    }

    /** The objective (and its progress) for a village during the current holiday, creating it on first use. */
    @Nullable
    public Progress progressFor(String villageKey) {
        CelebrationDirector.Festival f = festival();
        if (f == null || !enabled()) return null;
        String k = key(f.occurrence().key(), villageKey);
        Progress p = data.progress.get(k);
        if (p == null) {
            ObjectiveRules.Objective o = ObjectiveRules.choose(f.holiday().id(), f.occurrence().key(), villageKey);
            p = new Progress(o.item(), o.needed());
            data.progress.put(k, p);
            // Old years' objectives are only history; keep the file small.
            while (data.progress.size() > 256) data.progress.remove(data.progress.keySet().iterator().next());
            data.setDirty();
        }
        return p;
    }

    public void tick() {
        ServerLevel level = services.server().overworld();
        if (level.getGameTime() % 20 != 0) return;
        CelebrationDirector.Festival f = festival();
        if (f == null || !enabled()) {
            if (!bars.isEmpty()) {
                bars.values().forEach(ServerBossEvent::removeAllPlayers);
                bars.clear();
            }
            return;
        }
        Set<String> live = new HashSet<>();
        for (VillageRecord village : services.director().villages().all()) {
            if (!village.key.startsWith(level.dimension().location() + "|bell|")) continue;
            BlockPos centre = village.centre();
            if (!level.isLoaded(centre)) continue;
            Progress p = progressFor(village.key);
            if (p == null) continue;
            String k = key(f.occurrence().key(), village.key);
            live.add(k);
            ServerBossEvent bar = bars.computeIfAbsent(k, x -> new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.NOTCHED_10));
            bar.setName(p.completed
                    ? Component.translatable("holidayseasons.objective.done", describe(p))
                    : Component.translatable("holidayseasons.objective.bar", p.needed, describe(p), p.amount, p.needed));
            bar.setColor(p.completed ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.YELLOW);
            bar.setProgress(Math.min(1f, p.amount / (float) p.needed));
            for (ServerPlayer player : level.players()) {
                boolean near = player.blockPosition().distSqr(centre) < BAR_RANGE * BAR_RANGE;
                if (near && !bar.getPlayers().contains(player)) bar.addPlayer(player);
                else if (!near && bar.getPlayers().contains(player)) bar.removePlayer(player);
            }
        }
        bars.entrySet().removeIf(e -> {
            if (live.contains(e.getKey())) return false;
            e.getValue().removeAllPlayers();
            return true;
        });
    }

    public static Component describe(Progress p) {
        if (p.item.startsWith("#")) return Component.translatable("holidayseasons.objective.tag." + p.item.substring(p.item.indexOf(':') + 1));
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(p.item)).getDescription();
    }

    static boolean matches(ItemStack stack, String item) {
        if (stack.isEmpty()) return false;
        if (item.startsWith("#")) return stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(item.substring(1))));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item);
    }

    /** The village whose bell is at {@code bell}, if any. */
    @Nullable
    public VillageRecord villageAtBell(ServerLevel level, BlockPos bell) {
        for (VillageRecord r : services.director().villages().all()) {
            if (r.key.startsWith(level.dimension().location() + "|bell|") && r.centre().distManhattan(bell) <= 2) return r;
        }
        return null;
    }

    /**
     * A player uses {@code held} on a village bell. Returns how many items were taken (0 if the bell should just ring).
     */
    public int contribute(ServerLevel level, Player player, BlockPos bell, ItemStack held) {
        VillageRecord village = villageAtBell(level, bell);
        if (village == null) return 0;
        Progress p = progressFor(village.key);
        if (p == null) return 0;
        return contributeTo(level, player, village.key, village.centre(), p, held);
    }

    /** Hands {@code held} in towards {@code p}. Returns how many items were taken. */
    public int contributeTo(ServerLevel level, Player player, String villageKey, BlockPos centre, Progress p, ItemStack held) {
        BlockPos bell = centre;
        if (p.completed || !matches(held, p.item)) return 0;
        int take = ObjectiveRules.take(p.amount, p.needed, held.getCount());
        if (take <= 0) return 0;
        if (!player.getAbilities().instabuild) held.shrink(take);
        p.amount += take;
        p.contributors.merge(player.getUUID(), take, Integer::sum);
        data.setDirty();
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.6f, 1.4f);
        player.displayClientMessage(Component.translatable("holidayseasons.objective.thanks", take, describe(p), p.amount, p.needed), true);
        if (p.amount >= p.needed) complete(level, villageKey, centre, p);
        return take;
    }

    private void complete(ServerLevel level, String villageKey, BlockPos centre, Progress p) {
        p.completed = true;
        data.setDirty();
        long day = services.calendar().snapshot().epochDay();
        for (Map.Entry<UUID, Integer> c : p.contributors.entrySet()) {
            services.reputation().add(c.getKey(), villageKey, ReputationMath.Action.COMMUNITY_OBJECTIVE, day);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(c.getKey());
            if (player != null) {
                player.sendSystemMessage(Component.translatable("holidayseasons.objective.complete", describe(p)));
                HolidayTokens.award(player, 3, "holidayseasons.tokens.reason.objective");
            }
        }
        for (int i = 0; i < 4; i++) {
            CelebrationDirector.burst(level, centre.getCenter().add(level.random.nextGaussian() * 4, 14 + level.random.nextInt(6), level.random.nextGaussian() * 4));
        }
        level.playSound(null, centre, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1f);
        EverFest.LOGGER.info("EverFest: village objective complete at {} ({} contributors)", villageKey, p.contributors.size());
    }

    // ------------------------------------------------------------------ persistence

    public static final class Progress {
        public final String item;
        public final int needed;
        public int amount;
        public boolean completed;
        public final Map<UUID, Integer> contributors = new LinkedHashMap<>();

        public Progress(String item, int needed) {
            this.item = item;
            this.needed = needed;
        }
    }

    static final class Data extends SavedData {
        static final String FILE_ID = "holidayseasons_objectives";
        static final Factory<Data> FACTORY = new Factory<>(Data::new, Data::load, null);
        final Map<String, Progress> progress = new LinkedHashMap<>();

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (Tag t : tag.getList("objectives", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                Progress p = new Progress(c.getString("item"), c.getInt("needed"));
                p.amount = c.getInt("amount");
                p.completed = c.getBoolean("completed");
                for (Tag ct : c.getList("contributors", Tag.TAG_COMPOUND)) {
                    CompoundTag cc = (CompoundTag) ct;
                    p.contributors.put(cc.getUUID("id"), cc.getInt("n"));
                }
                d.progress.put(c.getString("key"), p);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            progress.forEach((k, p) -> {
                CompoundTag c = new CompoundTag();
                c.putString("key", k);
                c.putString("item", p.item);
                c.putInt("needed", p.needed);
                c.putInt("amount", p.amount);
                c.putBoolean("completed", p.completed);
                ListTag cs = new ListTag();
                p.contributors.forEach((id, n) -> {
                    CompoundTag cc = new CompoundTag();
                    cc.putUUID("id", id);
                    cc.putInt("n", n);
                    cs.add(cc);
                });
                c.put("contributors", cs);
                list.add(c);
            });
            tag.put("objectives", list);
            return tag;
        }
    }
}
