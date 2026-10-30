package com.holidayseasons.entity;

import com.holidayseasons.EverFest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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

    private EverFestEntities() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(GHOST.get(), GhostEntity.createAttributes().build());
            e.put(GOURDLING.get(), GourdlingEntity.createAttributes().build());
            e.put(SCARECROW.get(), ScarecrowEntity.createAttributes().build());
        });
    }
}
