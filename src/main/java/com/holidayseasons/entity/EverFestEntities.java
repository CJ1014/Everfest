package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EverFestEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, EverFest.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GhostEntity>> GHOST = TYPES.register("ghost",
            () -> EntityType.Builder.of(GhostEntity::new, MobCategory.MONSTER).sized(0.7f, 1.4f).clientTrackingRange(8).build("ghost"));
    public static final DeferredHolder<EntityType<?>, EntityType<GourdlingEntity>> GOURDLING = TYPES.register("gourdling",
            () -> EntityType.Builder.of(GourdlingEntity::new, MobCategory.MONSTER).sized(0.7f, 0.6f).clientTrackingRange(8).build("gourdling"));
    public static final DeferredHolder<EntityType<?>, EntityType<ScarecrowEntity>> SCARECROW = TYPES.register("scarecrow",
            () -> EntityType.Builder.of(ScarecrowEntity::new, MobCategory.MISC).sized(0.8f, 2.2f).clientTrackingRange(10).build("scarecrow"));
    public static final DeferredHolder<EntityType<?>, EntityType<ReindeerEntity>> REINDEER = TYPES.register("reindeer",
            () -> EntityType.Builder.of(ReindeerEntity::new, MobCategory.CREATURE).sized(0.9f, 1.6f).eyeHeight(1.45f).clientTrackingRange(10).build("reindeer"));
    public static final DeferredHolder<EntityType<?>, EntityType<SpringBunnyEntity>> SPRING_BUNNY = TYPES.register("spring_bunny",
            () -> EntityType.Builder.of(SpringBunnyEntity::new, MobCategory.CREATURE).sized(0.4f, 0.5f).clientTrackingRange(8).build("spring_bunny"));
    public static final DeferredHolder<EntityType<?>, EntityType<WildTurkeyEntity>> WILD_TURKEY = TYPES.register("wild_turkey",
            () -> EntityType.Builder.of(WildTurkeyEntity::new, MobCategory.CREATURE).sized(0.6f, 0.9f).eyeHeight(0.8f).clientTrackingRange(10).build("wild_turkey"));
    public static final DeferredHolder<EntityType<?>, EntityType<HolidayMerchantEntity>> HOLIDAY_MERCHANT = TYPES.register("holiday_merchant",
            () -> EntityType.Builder.of(HolidayMerchantEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10).build("holiday_merchant"));

    private EverFestEntities() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(GHOST.get(), GhostEntity.createAttributes().build());
            e.put(GOURDLING.get(), GourdlingEntity.createAttributes().build());
            e.put(SCARECROW.get(), ScarecrowEntity.createAttributes().build());
            e.put(REINDEER.get(), ReindeerEntity.createAttributes().build());
            e.put(SPRING_BUNNY.get(), SpringBunnyEntity.createAttributes().build());
            e.put(WILD_TURKEY.get(), WildTurkeyEntity.createAttributes().build());
            e.put(HOLIDAY_MERCHANT.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent e) -> {
            e.register(REINDEER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ReindeerEntity::checkReindeerSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(SPRING_BUNNY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    SpringBunnyEntity::checkSpringBunnySpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
            e.register(WILD_TURKEY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    WildTurkeyEntity::checkTurkeySpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }
}
