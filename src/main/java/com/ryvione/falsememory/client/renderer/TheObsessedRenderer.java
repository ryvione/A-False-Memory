package com.ryvione.falsememory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.ryvione.falsememory.entity.TheObsessedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.UUID;

public class TheObsessedRenderer extends MobRenderer<TheObsessedEntity, PlayerModel<TheObsessedEntity>> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FALLBACK =
        ResourceLocation.withDefaultNamespace("textures/entity/player/slim/steve.png");

    public TheObsessedRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(TheObsessedEntity entity) {
        String uuidStr = entity.getTargetPlayerUUID();
        if (uuidStr == null || uuidStr.isEmpty()) return FALLBACK;
        try {
            UUID targetUUID = UUID.fromString(uuidStr);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player p = mc.level.getPlayerByUUID(targetUUID);
                if (p instanceof AbstractClientPlayer cp) {
                    ResourceLocation skin = cp.getSkin().texture();
                    if (skin != null) return skin;
                }
            }
            if (mc.player != null) return mc.player.getSkin().texture();
        } catch (Exception e) {
            LOGGER.warn("[FalseMemory] Skin error: {}", e.getMessage());
        }
        return FALLBACK;
    }

    @Override
    public void render(TheObsessedEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        int phase = entity.getBehaviorPhase();
        if (phase == 0 && packedLight > 50) return;

        poseStack.pushPose();

        int light = switch (phase) {
            case 0 -> Math.min(packedLight, 15);
            case 1 -> Math.min(packedLight, 120);
            default -> packedLight;
        };

        if (phase < 3) poseStack.mulPose(Axis.ZP.rotationDegrees(7f));
        if (phase == 3) poseStack.scale(1.0f, 1.02f, 1.0f);

        float mimicYaw = entity.getMimicYaw();
        poseStack.mulPose(Axis.YP.rotationDegrees(-mimicYaw));
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, light);

        if (phase >= 2) {
            poseStack.pushPose();
            poseStack.translate(0.05, 0.02, 0.05);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
                Math.min(packedLight, 80));
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TheObsessedEntity entity) { return false; }
}