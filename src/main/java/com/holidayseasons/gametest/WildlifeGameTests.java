package com.holidayseasons.gametest;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.ReindeerEntity;
import com.holidayseasons.entity.SeasonalWildlife;
import com.holidayseasons.entity.SpringBunnyEntity;
import com.holidayseasons.entity.WildTurkeyEntity;
import com.holidayseasons.item.EverFestItems;
import com.holidayseasons.server.EverFestServer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** In-world tests for seasonal wildlife. Floor y=1, standing height y=2. */
@GameTestHolder(EverFest.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildlifeGameTests {
    private WildlifeGameTests() {
    }

    private static AABB plot(GameTestHelper helper) {
        return new AABB(helper.absolutePos(new BlockPos(0, 1, 0))).expandTowards(16, 8, 16);
    }

    private static void grassFloor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 600, batch = "everfest_wildlife")
    public static void reindeerBreedCalvesWithAParentsCoat(GameTestHelper helper) {
        ReindeerEntity a = helper.spawn(EverFestEntities.REINDEER.get(), new BlockPos(6, 2, 8));
        ReindeerEntity b = helper.spawn(EverFestEntities.REINDEER.get(), new BlockPos(9, 2, 8));
        a.setVariant(ReindeerEntity.FROSTY);
        b.setVariant(ReindeerEntity.FESTIVE);
        a.setInLove(null);
        b.setInLove(null);
        helper.succeedWhen(() -> {
            List<ReindeerEntity> calves = helper.getLevel().getEntitiesOfClass(ReindeerEntity.class, plot(helper), ReindeerEntity::isBaby);
            helper.assertTrue(calves.size() == 1, "one calf, found " + calves.size());
            int coat = calves.get(0).variant();
            helper.assertTrue(coat == ReindeerEntity.FROSTY || coat == ReindeerEntity.FESTIVE, "calf has a parent's coat, got " + coat);
            helper.getLevel().getEntitiesOfClass(ReindeerEntity.class, plot(helper).inflate(16)).forEach(Entity::discard);
        });
    }

    @GameTest(template = "plot_16x16", batch = "everfest_wildlife")
    public static void reindeerCoatSurvivesSaveAndLoad(GameTestHelper helper) {
        ReindeerEntity r = helper.spawn(EverFestEntities.REINDEER.get(), new BlockPos(8, 2, 8));
        r.setVariant(ReindeerEntity.FESTIVE);
        CompoundTag tag = new CompoundTag();
        r.saveWithoutId(tag);
        ReindeerEntity copy = EverFestEntities.REINDEER.get().create(helper.getLevel());
        helper.assertTrue(copy != null, "created");
        copy.load(tag);
        helper.assertTrue(copy.variant() == ReindeerEntity.FESTIVE, "festive coat kept");
        r.discard();
        helper.succeed();
    }

    @GameTest(template = "plot_16x16", batch = "everfest_wildlife")
    public static void springBunniesLayEggsOnlyInSpring(GameTestHelper helper) {
        SpringBunnyEntity bunny = helper.spawn(EverFestEntities.SPRING_BUNNY.get(), new BlockPos(8, 2, 8));
        bunny.setNoAi(true);
        bunny.layEggSoon();
        boolean spring = SpringBunnyEntity.springtime();
        helper.runAfterDelay(5, () -> {
            List<ItemEntity> eggs = helper.getLevel().getEntitiesOfClass(ItemEntity.class, plot(helper), i -> i.getItem().is(EverFestItems.EGG_TAG));
            helper.assertTrue(eggs.size() == (spring ? 1 : 0), "eggs " + eggs.size() + " while spring=" + spring);
            helper.assertTrue(bunny.removeWhenFarAway(200) == !spring, "wild bunnies leave only out of season");
            SpringBunnyEntity kit = bunny.getBreedOffspring(helper.getLevel(), bunny);
            helper.assertTrue(kit != null && kit.homebred() && !kit.removeWhenFarAway(200), "bred bunnies always stay");
            bunny.discard();
            eggs.forEach(Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "plot_16x16", timeoutTicks = 200, batch = "everfest_wildlife")
    public static void turkeysFlutterDownAndDropTurkey(GameTestHelper helper) {
        WildTurkeyEntity turkey = helper.spawn(EverFestEntities.WILD_TURKEY.get(), new BlockPos(8, 7, 8));
        float health = turkey.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(turkey.onGround(), "landed");
            helper.assertTrue(turkey.getHealth() == health, "no fall damage");
            turkey.kill();
            List<ItemEntity> meat = helper.getLevel().getEntitiesOfClass(ItemEntity.class, plot(helper),
                    i -> i.getItem().is(EverFestItems.RAW_TURKEY.get()) || i.getItem().is(EverFestItems.ROAST_TURKEY.get()));
            helper.assertTrue(meat.size() == 1, "drops one turkey");
        });
    }

    @GameTest(template = "plot_16x16", batch = "everfest_wildlife_cap")
    public static void seasonalVisitsRespectTheCap(GameTestHelper helper) {
        grassFloor(helper);
        var wildlife = EverFestServer.require().wildlife();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 2, 8));
        int first = wildlife.spawnGroupAt(helper.getLevel(), origin, SeasonalWildlife.Species.WILD_TURKEY, 3);
        helper.assertTrue(first == 3, "a flock of three arrived, got " + first);
        List<WildTurkeyEntity> flock = helper.getLevel().getEntitiesOfClass(WildTurkeyEntity.class, plot(helper));
        helper.assertTrue(flock.size() == 3, "all inside the plot");
        // A forced visit never pushes the population past the cap (5 by default).
        int total = 0;
        for (int i = 0; i < 5; i++) total += wildlife.trySpawn(helper.getLevel(), origin, SeasonalWildlife.Species.WILD_TURKEY, true);
        int around = wildlife.nearby(helper.getLevel(), origin, SeasonalWildlife.Species.WILD_TURKEY);
        helper.assertTrue(around <= 5, "at most 5 turkeys around, found " + around + " (" + total + " more arrived)");
        // Stone is not ground an animal can spawn on.
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        helper.assertTrue(wildlife.spawnGroupAt(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3)),
                SeasonalWildlife.Species.SPRING_BUNNY, 1) == 0, "no bunny on stone");
        helper.getLevel().getEntitiesOfClass(WildTurkeyEntity.class, new AABB(origin).inflate(64)).forEach(Entity::discard);
        helper.succeed();
    }
}
