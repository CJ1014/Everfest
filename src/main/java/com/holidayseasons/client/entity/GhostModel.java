package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.GhostEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/** A sheet ghost: round head, flowing robe, three wavering tails and reaching arms. */
public class GhostModel extends HierarchicalModel<GhostEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EverFest.id("ghost"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart[] tails = new ModelPart[3];

    public GhostModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        for (int i = 0; i < 3; i++) tails[i] = body.getChild("tail" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-5, 0, -5, 10, 10, 10), PartPose.offset(0, 6, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(6, 1, 0));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(-6, 1, 0));
        float[][] tailPos = {{0, 10, -3.5f}, {-3, 10, 3}, {3, 10, 3}};
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("tail" + i, CubeListBuilder.create().texOffs(40, 0 + i * 5).addBox(-2, 0, -0.5f, 4, 5, 1),
                    PartPose.offset(tailPos[i][0], tailPos[i][1], tailPos[i][2]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GhostEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        body.y = 6 + Mth.sin(age * 0.08f) * 1.5f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6f;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        float reach = e.isAggressive() || limbSwingAmount > 0.05f ? -1.3f : -0.4f;
        leftArm.xRot = reach + Mth.sin(age * 0.12f) * 0.15f;
        rightArm.xRot = reach + Mth.sin(age * 0.12f + 1.3f) * 0.15f;
        leftArm.zRot = -0.25f;
        rightArm.zRot = 0.25f;
        for (int i = 0; i < 3; i++) tails[i].xRot = 0.25f + Mth.sin(age * 0.2f + i * 2.1f) * 0.35f;
    }
}
