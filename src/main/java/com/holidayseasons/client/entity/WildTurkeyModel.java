package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.WildTurkeyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A plump turkey whose banded tail fans up into a wheel when it struts. */
public class WildTurkeyModel extends HierarchicalModel<WildTurkeyEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EverFest.id("wild_turkey"), "main");
    private static final float TAIL_FOLDED = -1.9f;
    private static final float TAIL_FANNED = -0.2f;
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public WildTurkeyModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3.5f, -4, 6, 7, 8), PartPose.offset(0, 15.5f, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 0).addBox(-5, -9, 0, 10, 9, 1),
                PartPose.offsetAndRotation(0, 14, 3.5f, TAIL_FOLDED, 0, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(50, 0).addBox(-1, -5, -1, 2, 5, 2)
                        .texOffs(58, 0).addBox(-0.5f, -4.5f, -3, 1, 1, 2)
                        .texOffs(58, 3).addBox(-0.5f, -3.5f, -2, 1, 3, 1),
                PartPose.offset(0, 13, -3.5f));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 19).addBox(-1, 0, -3, 1, 5, 6), PartPose.offset(-3, 13, 0));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 19).mirror().addBox(0, 0, -3, 1, 5, 6), PartPose.offset(3, 13, 0));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 15).addBox(-0.5f, 0, -0.5f, 1, 4, 1)
                .texOffs(4, 15).addBox(-1.5f, 4, -2.5f, 3, 1, 3);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.5f, 19, 0.5f));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.5f, 19, 0.5f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(WildTurkeyEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float fan = Mth.lerp(partial, e.oFan, e.fan);
        tail.xRot = Mth.lerp(fan, TAIL_FOLDED, TAIL_FANNED);
        tail.xScale = 0.55f + 0.45f * fan;
        tail.visible = !young;
        // Strutting toms puff up and tuck their head back; walking birds bob their head.
        head.z = -3.5f + 1.2f * fan;
        head.y = 13 + Mth.cos(limbSwing * 1.3f) * 0.6f * limbSwingAmount;
        float flapAngle = (Mth.sin(Mth.lerp(partial, e.oFlap, e.flap)) + 1f) * Mth.lerp(partial, e.oFlapSpeed, e.flapSpeed);
        rightWing.zRot = flapAngle + 0.25f * fan;
        leftWing.zRot = -flapAngle - 0.25f * fan;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
    }
}
