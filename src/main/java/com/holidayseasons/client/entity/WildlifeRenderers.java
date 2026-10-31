package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.client.ClientHolidayState;
import com.holidayseasons.config.ClientConfig;
import com.holidayseasons.entity.EverFestEntities;
import com.holidayseasons.entity.FestiveOutfits;
import com.holidayseasons.entity.ReindeerEntity;
import com.holidayseasons.entity.SpringBunnyEntity;
import com.holidayseasons.entity.WildTurkeyEntity;
import com.holidayseasons.holiday.HolidayPhase;
import com.holidayseasons.holiday.HolidayStatus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.HashSet;
import java.util.Set;

/** Renderers for EverFest's seasonal wildlife, and the festive outfits some vanilla animals wear on holidays. */
public final class WildlifeRenderers {
    private static final String[] REINDEER_COATS = {"brown", "frosty", "festive"};
    private static final String[] BUNNY_COATS = {"cream", "blush", "sky", "lilac"};

    private WildlifeRenderers() {
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ReindeerModel.LAYER, ReindeerModel::createLayer);
        e.registerLayerDefinition(WildTurkeyModel.LAYER, WildTurkeyModel::createLayer);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EverFestEntities.REINDEER.get(), ReindeerRenderer::new);
        e.registerEntityRenderer(EverFestEntities.SPRING_BUNNY.get(), SpringBunnyRenderer::new);
        e.registerEntityRenderer(EverFestEntities.WILD_TURKEY.get(), WildTurkeyRenderer::new);
        e.registerEntityRenderer(EverFestEntities.HOLIDAY_MERCHANT.get(), MerchantRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addOutfitLayers(EntityRenderersEvent.AddLayers e) {
        for (EntityType<?> type : new EntityType<?>[]{EntityType.COW, EntityType.WOLF, EntityType.RABBIT}) {
            if (e.getRenderer(type) instanceof LivingEntityRenderer r) r.addLayer(new OutfitLayer(r));
        }
        if (e.getRenderer(EntityType.SHEEP) instanceof LivingEntityRenderer r) {
            r.addLayer(new SheepOutfitLayer(r, new SheepFurModel<>(e.getEntityModels().bakeLayer(ModelLayers.SHEEP_FUR))));
        }
    }

    // ------------------------------------------------------------------------------------------- reindeer

    static final class ReindeerRenderer extends MobRenderer<ReindeerEntity, ReindeerModel> {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[REINDEER_COATS.length];
        private static final ResourceLocation NOSE = EverFest.id("textures/entity/reindeer/nose_glow.png");

        static {
            for (int i = 0; i < REINDEER_COATS.length; i++) TEXTURES[i] = EverFest.id("textures/entity/reindeer/" + REINDEER_COATS[i] + ".png");
        }

        ReindeerRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ReindeerModel(ctx.bakeLayer(ReindeerModel.LAYER)), 0.6f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, ReindeerEntity e, float limbSwing,
                                   float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
                    if (e.variant() != ReindeerEntity.FESTIVE || e.isInvisible()) return;
                    VertexConsumer glow = buffers.getBuffer(RenderType.eyes(NOSE));
                    getParentModel().renderToBuffer(pose, glow, 0xF000F0, OverlayTexture.NO_OVERLAY);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(ReindeerEntity e) {
            return TEXTURES[Math.floorMod(e.variant(), TEXTURES.length)];
        }

        @Override
        protected void scale(ReindeerEntity e, PoseStack pose, float partialTick) {
            if (e.isBaby()) pose.scale(0.6f, 0.6f, 0.6f);
        }
    }

    // ------------------------------------------------------------------------------------------- spring bunny

    static final class SpringBunnyRenderer extends MobRenderer<SpringBunnyEntity, RabbitModel<SpringBunnyEntity>> {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[BUNNY_COATS.length];

        static {
            for (int i = 0; i < BUNNY_COATS.length; i++) TEXTURES[i] = EverFest.id("textures/entity/spring_bunny/" + BUNNY_COATS[i] + ".png");
        }

        SpringBunnyRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new RabbitModel<>(ctx.bakeLayer(ModelLayers.RABBIT)), 0.3f);
        }

        @Override
        public ResourceLocation getTextureLocation(SpringBunnyEntity e) {
            return TEXTURES[Math.floorMod(e.coat(), TEXTURES.length)];
        }
    }

    // ------------------------------------------------------------------------------------------- turkey

    static final class WildTurkeyRenderer extends MobRenderer<WildTurkeyEntity, WildTurkeyModel> {
        private static final ResourceLocation TEXTURE = EverFest.id("textures/entity/wild_turkey.png");

        WildTurkeyRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new WildTurkeyModel(ctx.bakeLayer(WildTurkeyModel.LAYER)), 0.4f);
        }

        @Override
        public ResourceLocation getTextureLocation(WildTurkeyEntity e) {
            return TEXTURE;
        }

        @Override
        protected void scale(WildTurkeyEntity e, PoseStack pose, float partialTick) {
            if (e.isBaby()) pose.scale(0.5f, 0.5f, 0.5f);
        }
    }

    // ------------------------------------------------------------------------------------------- festive outfits

    static Set<String> festiveHolidays() {
        Set<String> ids = new HashSet<>();
        for (HolidayStatus s : ClientHolidayState.statuses()) {
            if (s.phase() == HolidayPhase.PREPARING || s.phase() == HolidayPhase.ACTIVE) ids.add(s.id());
        }
        return ids;
    }

    static ResourceLocation outfitFor(LivingEntity e) {
        if (!ClientConfig.SPEC.isLoaded() || !ClientConfig.INSTANCE.festiveAnimalOutfits.get() || e.isInvisible()) return null;
        FestiveOutfits.Outfit o = FestiveOutfits.outfitFor(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), e.getUUID(), festiveHolidays());
        return o == null ? null : EverFest.id("textures/entity/outfit/" + o.texture + ".png");
    }

    /** Paints an outfit over the animal's own model (like a wolf's collar). */
    static final class OutfitLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        OutfitLayer(RenderLayerParent<T, M> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            ResourceLocation tex = outfitFor(e);
            if (tex != null) renderColoredCutoutModel(getParentModel(), tex, pose, buffers, light, e, -1);
        }
    }

    /** Sheep wear their scarf over the wool when they have it, otherwise on the shorn body. */
    static final class SheepOutfitLayer extends RenderLayer<Sheep, SheepModel<Sheep>> {
        private final SheepFurModel<Sheep> fur;

        SheepOutfitLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> parent, SheepFurModel<Sheep> fur) {
            super(parent);
            this.fur = fur;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Sheep sheep, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            ResourceLocation tex = outfitFor(sheep);
            if (tex == null) return;
            if (sheep.isSheared()) {
                renderColoredCutoutModel(getParentModel(), tex, pose, buffers, light, sheep, -1);
                return;
            }
            coloredCutoutModelCopyLayerRender(getParentModel(), fur, tex, pose, buffers, light, sheep, limbSwing, limbSwingAmount,
                    age, headYaw, headPitch, partialTick, -1);
        }
    }
}
