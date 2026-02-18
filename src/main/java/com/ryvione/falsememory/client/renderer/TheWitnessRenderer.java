package com.ryvione.falsememory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryvione.falsememory.entity.TheWitnessEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TheWitnessRenderer extends MobRenderer<TheWitnessEntity, HumanoidModel<TheWitnessEntity>> {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("falsememory", "textures/entity/the_witness.png");

    public TheWitnessRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)), 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(TheWitnessEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(TheWitnessEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.8f, 0.8f, 0.8f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
            Math.min(packedLight, 40));
        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TheWitnessEntity entity) { return false; }
}