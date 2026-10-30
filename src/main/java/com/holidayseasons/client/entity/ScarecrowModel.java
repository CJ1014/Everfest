package com.holidayseasons.client.entity;

import com.holidayseasons.EverFest;
import com.holidayseasons.entity.ScarecrowEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A straw scarecrow on a post with a pumpkin head and a battered hat; the head tilts eerily. */
public class ScarecrowModel extends HierarchicalModel<ScarecrowEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EverFest.id("scarecrow"), "main");
    private final ModelPart root;
    private final ModelPart head;

    public ScarecrowModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("frame").getChild("head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition frame = mesh.getRoot().addOrReplaceChild("frame", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-1, -30, -1, 2, 30, 2)
                .texOffs(0, 16).addBox(-4, -28, -2, 8, 12, 4)
                .texOffs(0, 32).addBox(-12, -27, -1, 24, 2, 2)
                .texOffs(24, 16).addBox(-13, -28, -1.5f, 3, 4, 3)
                .texOffs(24, 16).addBox(10, -28, -1.5f, 3, 4, 3)
                .texOffs(24, 24).addBox(-3, -16, -2.5f, 6, 3, 5), PartPose.offset(0, 24, 0));
        PartDefinition head = frame.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.offset(0, -28, 0));
        head.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 40).addBox(-6, -9, -6, 12, 1, 12), PartPose.ZERO);
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(32, 0).addBox(-3, -13, -3, 6, 4, 6), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.08f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ScarecrowEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.zRot = 0.18f + Mth.sin(age * 0.03f) * 0.03f;
        head.xRot = 0.1f;
    }
}
