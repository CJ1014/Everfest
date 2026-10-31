package com.holidayseasons.reputation;

import com.holidayseasons.EverFest;
import com.holidayseasons.ai.CelebrationDirector;
import com.holidayseasons.config.ServerConfig;
import com.holidayseasons.economy.MerchantTrades;
import com.holidayseasons.entity.ReindeerEntity;
import com.holidayseasons.entity.SpringBunnyEntity;
import com.holidayseasons.entity.WildTurkeyEntity;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.network.CollectionPayload;
import com.holidayseasons.server.EverFestServer;
import com.holidayseasons.village.VillageRecord;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;

/**
 * Keeps every player's Holiday Collection Book up to date and grants the matching advancements. Cheap by design:
 * players are looked at once every two seconds (inventory, nearby wildlife, whether they are in a celebrating village);
 * everything else is recorded where it happens.
 */
public final class CollectionService {
    private static final String[] BUNNY_COATS = {"cream", "blush", "sky", "lilac"};
    private static final String[] REINDEER_COATS = {"brown", "frosty", "festive"};
    private final EverFestServer services;
    private final CollectionData data;

    public CollectionService(EverFestServer services) {
        this.services = services;
        this.data = services.server().overworld().getDataStorage().computeIfAbsent(CollectionData.FACTORY, CollectionData.FILE_ID);
    }

    public CollectionData.Book book(Player p) {
        return data.book(p.getUUID());
    }

    // ------------------------------------------------------------------ recording

    public void count(Player p, String stat, int amount) {
        CollectionData.Book b = book(p);
        int now = b.stats.merge(stat, amount, Integer::sum);
        data.setDirty();
        if (stat.equals("eggs_found") && now >= 10) EverFestAdvancements.award(p, EverFestAdvancements.EGG_HUNTER);
        checkCollector(p, b);
    }

    public void encounter(Player p, String encounter) {
        if (book(p).encounters.add(encounter)) {
            data.setDirty();
            if (p instanceof ServerPlayer sp && sp.connection != null) sp.displayClientMessage(Component.translatable("holidayseasons.collection.new_entry",
                    Component.translatable("holidayseasons.collection.encounter." + encounter)), true);
            checkCollector(p, book(p));
        }
    }

    public void discover(Player p, String wildlife) {
        if (book(p).wildlife.add(wildlife)) {
            data.setDirty();
            if (p instanceof ServerPlayer sp && sp.connection != null) sp.displayClientMessage(Component.translatable("holidayseasons.collection.new_entry",
                    Component.translatable("holidayseasons.collection.wildlife." + wildlife)), true);
            if (wildlife.equals("reindeer.festive")) EverFestAdvancements.award(p, EverFestAdvancements.RED_NOSED);
            checkCollector(p, book(p));
        }
    }

    public void collect(Player p, String itemId) {
        if (Collectibles.isCollectible(itemId) && book(p).items.add(itemId)) {
            data.setDirty();
            checkCollector(p, book(p));
        }
    }

    public void celebrated(Player p, String holidayId, int year) {
        CollectionData.Book b = book(p);
        if (b.celebrated.computeIfAbsent(holidayId, h -> new java.util.TreeSet<>()).add(year)) {
            data.setDirty();
            Set<String> enabled = new java.util.HashSet<>();
            for (Map.Entry<String, Boolean> e : ServerConfig.BUILT_IN_HOLIDAYS.entrySet()) {
                boolean on = ServerConfig.SPEC.isLoaded() ? ServerConfig.INSTANCE.holidays.get(e.getKey()).enabled.get() : e.getValue();
                if (on) enabled.add(EverFest.MOD_ID + ":" + e.getKey());
            }
            if (!enabled.isEmpty() && b.celebrated.keySet().containsAll(enabled)) EverFestAdvancements.award(p, EverFestAdvancements.YEAR_OF_FESTIVALS);
        }
    }

    private void checkCollector(Player p, CollectionData.Book b) {
        if (b.items.size() >= Collectibles.itemCount()) EverFestAdvancements.award(p, EverFestAdvancements.COLLECTOR);
    }

    /** Called when reputation changes, for the Local Legend advancement. */
    public void reputationChanged(Player p, int reputation) {
        if (ReputationMath.tier(reputation) == ReputationMath.Tier.LEGEND) EverFestAdvancements.award(p, EverFestAdvancements.LOCAL_LEGEND);
    }

    // ------------------------------------------------------------------ periodic look

    public void tick() {
        var server = services.server();
        if (server.getTickCount() % 40 != 0) return;
        CelebrationDirector.Festival f = services.director().festival();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            scanInventory(p);
            scanWildlife(p);
            for (int rep : services.reputation().of(p.getUUID()).values()) reputationChanged(p, rep);
            if (f != null && f.phase() == HolidayPhase.ACTIVE && inCelebratingVillage(p)) {
                celebrated(p, f.holiday().id(), f.occurrence().year);
            }
        }
    }

    public void scanInventory(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.isEmpty()) continue;
            collect(p, idOf(s));
        }
        for (ItemStack s : p.getInventory().armor) if (!s.isEmpty()) collect(p, idOf(s));
        for (ItemStack s : p.getInventory().offhand) if (!s.isEmpty()) collect(p, idOf(s));
    }

    public static String idOf(ItemStack s) {
        if (s.is(Items.SOUL_LANTERN) && s.has(DataComponents.CUSTOM_NAME)
                && s.get(DataComponents.CUSTOM_NAME).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("item.holidayseasons.peddlers_lantern")) {
            return Collectibles.PEDDLERS_LANTERN;
        }
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    public void scanWildlife(ServerPlayer p) {
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(16), e -> e instanceof ReindeerEntity
                || e instanceof SpringBunnyEntity || e instanceof WildTurkeyEntity)) {
            if (!p.hasLineOfSight(e)) continue;
            if (e instanceof ReindeerEntity r) discover(p, "reindeer." + REINDEER_COATS[Math.floorMod(r.variant(), 3)]);
            else if (e instanceof SpringBunnyEntity b) discover(p, "spring_bunny." + BUNNY_COATS[Math.floorMod(b.coat(), 4)]);
            else discover(p, "wild_turkey");
        }
    }

    private boolean inCelebratingVillage(ServerPlayer p) {
        String dim = p.level().dimension().location() + "|bell|";
        for (VillageRecord r : services.director().villages().all()) {
            if (r.key.startsWith(dim) && r.centre().distSqr(p.blockPosition()) < 64 * 64) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ book

    /** Sends the player's book to their client, which opens it. */
    public void open(ServerPlayer p) {
        scanInventory(p);
        PacketDistributor.sendToPlayer(p, new CollectionPayload(book(p).toTag()));
    }

    /** For the peddler's lantern check in tests. */
    public static ItemStack peddlersLantern() {
        return MerchantTrades.peddlersLantern();
    }

    static ServerLevel overworld(EverFestServer s) {
        return s.server().overworld();
    }
}
