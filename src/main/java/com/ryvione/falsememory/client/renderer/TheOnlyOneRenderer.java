package com.ryvione.falsememory.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.entity.TheOnlyOneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.UUID;

public class TheOnlyOneRenderer extends MobRenderer<TheOnlyOneEntity, PlayerModel<TheOnlyOneEntity>> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FALLBACK =
        ResourceLocation.withDefaultNamespace("textures/entity/player/slim/steve.png");

    public TheOnlyOneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(TheOnlyOneEntity entity) {
        String uuidStr = entity.getTargetUUID();
        if (uuidStr == null || uuidStr.isEmpty()) return FALLBACK;
        try {
            UUID uuid = UUID.fromString(uuidStr);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player p = mc.level.getPlayerByUUID(uuid);
                if (p instanceof AbstractClientPlayer cp) {
                    ResourceLocation skin = cp.getSkin().texture();
                    if (skin != null) return skin;
                }
            }
            if (mc.player != null) return mc.player.getSkin().texture();
        } catch (Exception e) {
            LOGGER.warn("[FalseMemory] Skin error (OnlyOne): {}", e.getMessage());
        }
        return FALLBACK;
    }

    @Override
    public void render(TheOnlyOneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.scale(1.0f, 1.0f, 1.0f);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (entity.isInManhunt()) {
            poseStack.pushPose();
            poseStack.scale(1.02f, 1.04f, 1.02f);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
                Math.max(packedLight, 200));
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(TheOnlyOneEntity entity) { return false; }
}