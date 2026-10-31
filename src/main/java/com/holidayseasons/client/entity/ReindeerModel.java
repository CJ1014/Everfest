package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.ReindeerEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A slender deer: long legs, a forward-leaning neck, a short tail and branching antlers (adults only). */
public class ReindeerModel extends HierarchicalModel<ReindeerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EverFest.id("reindeer"), "main");
    private static final float NECK_TILT = 0.45f;
    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart antlers;
    private final ModelPart tail;
    private final ModelPart rightFront;
    private final ModelPart leftFront;
    private final ModelPart rightHind;
    private final ModelPart leftHind;

    public ReindeerModel(ModelPart root) {
        this.root = root;
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.antlers = head.getChild("antlers");
        this.tail = root.getChild("tail");
        this.rightFront = root.getChild("right_front_leg");
        this.leftFront = root.getChild("left_front_leg");
        this.rightHind = root.getChild("right_hind_leg");
        this.leftHind = root.getChild("left_hind_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -7, 8, 8, 14), PartPose.offset(0, 10, 0));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(44, 0).addBox(-2, -8, -2, 4, 8, 4),
                PartPose.offsetAndRotation(0, 8, -5.5f, NECK_TILT, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 22).addBox(-2.5f, -3, -5, 5, 5, 7)
                        .texOffs(24, 22).addBox(-1.5f, -1, -8, 3, 3, 3)
                        .texOffs(36, 22).addBox(-1, -1, -8.5f, 2, 2, 1),
                PartPose.offsetAndRotation(0, -7, 0, -NECK_TILT, 0, 0));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(42, 22).addBox(-3, -1, -0.5f, 3, 2, 1),
                PartPose.offsetAndRotation(-2.5f, -2, 0, 0, 0, 0.35f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(42, 22).mirror().addBox(0, -1, -0.5f, 3, 2, 1),
                PartPose.offsetAndRotation(2.5f, -2, 0, 0, 0, -0.35f));
        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(), PartPose.offset(0, -3, 0));
        antlers.addOrReplaceChild("right_antler", CubeListBuilder.create()
                        .texOffs(44, 12).addBox(-0.5f, -7, -0.5f, 1, 7, 1)
                        .texOffs(48, 12).addBox(-0.5f, -4.5f, -3.5f, 1, 1, 3)
                        .texOffs(48, 16).addBox(-3.5f, -7.5f, -0.5f, 3, 1, 1),
                PartPose.offsetAndRotation(-1.5f, 0, 0.5f, -0.2f, 0, -0.35f));
        antlers.addOrReplaceChild("left_antler", CubeListBuilder.create().mirror()
                        .texOffs(44, 12).addBox(-0.5f, -7, -0.5f, 1, 7, 1)
                        .texOffs(48, 12).addBox(-0.5f, -4.5f, -3.5f, 1, 1, 3)
                        .texOffs(48, 16).addBox(0.5f, -7.5f, -0.5f, 3, 1, 1),
                PartPose.offsetAndRotation(1.5f, 0, 0.5f, -0.2f, 0, 0.35f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 34).addBox(-1.5f, 0, 0, 3, 3, 1),
                PartPose.offsetAndRotation(0, 6.5f, 7, 0.5f, 0, 0));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 34).addBox(-1, 0, -1, 2, 10, 2);
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5f, 14, -5));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.5f, 14, -5));
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5f, 14, 5.5f));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.5f, 14, 5.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ReindeerEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        antlers.visible = !young;
        neck.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6f;
        head.xRot = -NECK_TILT + headPitch * Mth.DEG_TO_RAD * 0.6f;
        float swing = limbSwing * 0.6662f;
        rightHind.xRot = Mth.cos(swing) * 1.2f * limbSwingAmount;
        leftHind.xRot = Mth.cos(swing + Mth.PI) * 1.2f * limbSwingAmount;
        rightFront.xRot = Mth.cos(swing + Mth.PI) * 1.2f * limbSwingAmount;
        leftFront.xRot = Mth.cos(swing) * 1.2f * limbSwingAmount;
        tail.yRot = Mth.sin(age * 0.15f) * 0.08f;
    }
}
