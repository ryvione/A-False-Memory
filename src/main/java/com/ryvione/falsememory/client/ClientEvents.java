package com.ryvione.falsememory.client;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.FalseMemory;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.client.renderer.TheObsessedRenderer;
import com.ryvione.falsememory.client.renderer.TheOnlyOneRenderer;
import com.ryvione.falsememory.client.renderer.TheWitnessRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = FalseMemory.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @EventBusSubscriber(modid = FalseMemory.MOD_ID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.THE_OBSESSED.get(), TheObsessedRenderer::new);
            event.registerEntityRenderer(ModEntities.THE_ONLY_ONE.get(), TheOnlyOneRenderer::new);
            event.registerEntityRenderer(ModEntities.THE_WITNESS.get(),
                com.ryvione.falsememory.client.renderer.TheWitnessRenderer::new);
        }
    }

    @SubscribeEvent
    public static void onClientLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) return;
        SanityEffects.onClientTick();
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        SanityEffects.renderOverlay(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }

    @SubscribeEvent
    public static void onClientTick2(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide()) return;
        com.ryvione.falsememory.client.CorruptedSleepHandler.tick();
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        float sx = SanityEffects.getCameraShakeX();
        float sy = SanityEffects.getCameraShakeY();
        if (sx != 0 || sy != 0) {
            event.setYaw(event.getYaw() + sx);
            event.setPitch(event.getPitch() + sy);
        }
    }
}