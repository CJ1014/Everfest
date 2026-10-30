package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.GourdlingEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** A little pumpkin on stubby legs that squashes when it lands. */
public class GourdlingModel extends HierarchicalModel<GourdlingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EverFest.id("gourdling"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public GourdlingModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -8, -5, 10, 8, 10), PartPose.offset(0, 22, 0));
        body.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(40, 0).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(0, -8, 0));
        body.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(40, 5).addBox(0, -1, -1, 4, 1, 2), PartPose.offsetAndRotation(1, -9, 0, 0, 0, -0.35f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 18).addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(2.5f, 22, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 18).addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(-2.5f, 22, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GourdlingEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float squish = Mth.lerp(partial, e.squishO, e.squish);
        body.yScale = 1f - 0.25f * squish;
        body.xScale = body.zScale = 1f + 0.12f * squish;
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        leftLeg.xRot = Mth.cos(limbSwing * 1.3f) * 1.2f * limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 1.3f + Mth.PI) * 1.2f * limbSwingAmount;
    }
}
