package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.economy.MerchantTrades;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.ReindeerEntity;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.reputation.Collectibles;
import com.holidayseasons.reputation.CollectionData;
import com.holidayseasons.reputation.CollectionService;
import com.holidayseasons.reputation.EverFestAdvancements;
import com.holidayseasons.server.EverFestServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Tests for the Holiday Collection Book and EverFest's advancements. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private ProgressionGameTests() {
    }

    /** A server player with real advancement tracking (NeoForge's FakePlayer never earns advancements), not logged in. */
    private static ServerPlayer fakePlayer(GameTestHelper helper) {
        ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "fest_tester"),
                net.minecraft.server.level.ClientInformation.createDefault());
        BlockPos at = helper.absolutePos(new BlockPos(8, 2, 8));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        return p;
    }

    private static boolean has(ServerPlayer p, String id) {
        AdvancementHolder adv = p.getServer().getAdvancements().get(EverFest.id("everfest/" + id));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    @GameTest(template = "plot_16x16")
    public static void everyAdvancementLoads(GameTestHelper helper) {
        for (String id : List.of("root", EverFestAdvancements.TRICK_OR_TREAT, EverFestAdvancements.HOME_SWEET_HOME, EverFestAdvancements.WELL_CHOSEN,
                EverFestAdvancements.EGG_HUNTER, "golden_find", EverFestAdvancements.HAPPY_NEW_YEAR, EverFestAdvancements.COMMUNITY,
                EverFestAdvancements.LIFE_OF_THE_PARTY, EverFestAdvancements.THE_PEDDLER, EverFestAdvancements.RESTLESS_GRAVES,
                EverFestAdvancements.RED_NOSED, EverFestAdvancements.LOCAL_LEGEND, "a_new_chapter", EverFestAdvancements.COLLECTOR,
                EverFestAdvancements.YEAR_OF_FESTIVALS)) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(EverFest.id("everfest/" + id)) != null, id + " loads");
        }
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void bookCountsFeatsAndGrantsAdvancementsOnce(GameTestHelper helper) {
        CollectionService c = EverFestServer.require().collection();
        ServerPlayer p = fakePlayer(helper);
        c.count(p, "eggs_found", 9);
        helper.assertTrue(!has(p, EverFestAdvancements.EGG_HUNTER), "not yet at 9 eggs");
        c.count(p, "eggs_found", 1);
        helper.assertTrue(has(p, EverFestAdvancements.EGG_HUNTER), "Egg Hunter at 10 eggs");
        helper.assertTrue(!EverFestAdvancements.award(p, EverFestAdvancements.EGG_HUNTER), "granted only once");
        c.encounter(p, "midnight_peddler");
        c.encounter(p, "midnight_peddler");
        CollectionData.Book book = c.book(p);
        helper.assertTrue(book.encounters.size() == 1, "encounters are a set");
        CollectionData.Book copy = CollectionData.Book.fromTag(book.toTag());
        helper.assertTrue(copy.stat("eggs_found") == 10 && copy.encounters.equals(book.encounters), "book survives save/load");
        helper.succeed();
    }

    @GameTest(template = "plot_16x16")
    public static void inventoryAndWildlifeFillTheBook(GameTestHelper helper) {
        CollectionService c = EverFestServer.require().collection();
        ServerPlayer p = fakePlayer(helper);
        p.getInventory().add(new ItemStack(EverFestItems.SNOW_GLOBE.get()));
        p.getInventory().add(new ItemStack(Items.DIRT));
        p.getInventory().add(MerchantTrades.peddlersLantern());
        c.scanInventory(p);
        CollectionData.Book book = c.book(p);
        helper.assertTrue(book.items.contains("holidayseasons:snow_globe"), "snow globe collected");
        helper.assertTrue(book.items.contains(Collectibles.PEDDLERS_LANTERN), "the named lantern counts as the peddler's lantern");
        helper.assertTrue(!book.items.contains("minecraft:dirt") && !book.items.contains("minecraft:soul_lantern"), "only catalogue items");
        ReindeerEntity r = helper.spawn(EverFestEntities.REINDEER.get(), new BlockPos(11, 2, 8));
        r.setVariant(ReindeerEntity.FESTIVE);
        r.setNoAi(true);
        c.scanWildlife(p);
        helper.assertTrue(book.wildlife.contains("reindeer.festive"), "red-nosed reindeer discovered");
        helper.assertTrue(has(p, EverFestAdvancements.RED_NOSED), "A Very Shiny Nose");
        r.discard();
        helper.succeed();
    }
}
