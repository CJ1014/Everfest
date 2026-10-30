package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.GhostEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderer and model-layer registration for EverFest's creatures. */
public final class HalloweenRenderers {
    private HalloweenRenderers() {
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(GhostModel.LAYER, GhostModel::createLayer);
        e.registerLayerDefinition(GourdlingModel.LAYER, GourdlingModel::createLayer);
        e.registerLayerDefinition(ScarecrowModel.LAYER, ScarecrowModel::createLayer);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EverFestEntities.GHOST.get(), GhostRenderer::new);
        e.registerEntityRenderer(EverFestEntities.GOURDLING.get(), ctx -> new SimpleRenderer<>(ctx, new GourdlingModel(ctx.bakeLayer(GourdlingModel.LAYER)), 0.4f, "gourdling"));
        e.registerEntityRenderer(EverFestEntities.SCARECROW.get(), ctx -> new SimpleRenderer<>(ctx, new ScarecrowModel(ctx.bakeLayer(ScarecrowModel.LAYER)), 0.3f, "scarecrow"));
    }

    static final class GhostRenderer extends MobRenderer<GhostEntity, GhostModel> {
        private static final ResourceLocation TEXTURE = EverFest.id("textures/entity/ghost.png");

        GhostRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GhostModel(ctx.bakeLayer(GhostModel.LAYER)), 0.0f);
        }

        @Override
        public ResourceLocation getTextureLocation(GhostEntity entity) {
            return TEXTURE;
        }

        @Override
        protected int getBlockLightLevel(GhostEntity entity, net.minecraft.core.BlockPos pos) {
            return 10;
        }
    }

    static final class SimpleRenderer<T extends net.minecraft.world.entity.Mob, M extends net.minecraft.client.model.EntityModel<T>> extends MobRenderer<T, M> {
        private final ResourceLocation texture;

        SimpleRenderer(EntityRendererProvider.Context ctx, M model, float shadow, String name) {
            super(ctx, model, shadow);
            this.texture = EverFest.id("textures/entity/" + name + ".png");
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }
    }
}
