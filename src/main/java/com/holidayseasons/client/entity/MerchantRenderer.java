package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.HolidayMerchantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * Holiday merchants use the villager-shaped trader model (with its hat and crossed-arms layers) and EverFest's own
 * robes for each occasion; the Midnight Peddler's eyes glow in the dark.
 */
public class MerchantRenderer extends WanderingTraderRenderer {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[HolidayMerchantEntity.Look.values().length];
    private static final ResourceLocation PEDDLER_EYES = EverFest.id("textures/entity/merchant/midnight_eyes.png");

    static {
        for (HolidayMerchantEntity.Look l : HolidayMerchantEntity.Look.values()) {
            TEXTURES[l.ordinal()] = EverFest.id("textures/entity/merchant/" + l.texture() + ".png");
        }
    }

    public MerchantRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, WanderingTrader e, float limbSwing,
                               float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
                if (!(e instanceof HolidayMerchantEntity m) || m.look() != HolidayMerchantEntity.Look.MIDNIGHT || e.isInvisible()) return;
                VillagerModel<WanderingTrader> model = getParentModel();
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(PEDDLER_EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingTrader e) {
        return e instanceof HolidayMerchantEntity m ? TEXTURES[m.look().ordinal()] : super.getTextureLocation(e);
    }
}
