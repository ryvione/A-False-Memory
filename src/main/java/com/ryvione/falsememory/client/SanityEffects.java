package com.ryvione.falsememory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class SanityEffects {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG = new Random();
    
    private static int currentTier = 0;
    private static boolean inManhunt = false;
    private static float vignetteAlpha = 0f;
    private static int staticFramesRemaining = 0;
    private static int shakeFramesRemaining = 0;
    private static float shakeIntensity = 0f;
    private static int watchedOverlayTimer = 0;
    private static int invertFrames = 0;
    private static long lastEffectTick = 0;
    public static int getCurrentTier() { return currentTier; }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        long tick = mc.level.getGameTime();

        if (vignetteAlpha > 0) vignetteAlpha = Math.max(0, vignetteAlpha - 0.005f);
        if (staticFramesRemaining > 0) staticFramesRemaining--;
        if (shakeFramesRemaining > 0) {
            shakeFramesRemaining--;
            shakeIntensity = Math.max(0, shakeIntensity - 0.02f);
        }
        if (watchedOverlayTimer > 0) watchedOverlayTimer--;
        if (invertFrames > 0) invertFrames--;

        if (inManhunt) {
            if (vignetteAlpha < 0.6f) vignetteAlpha = 0.6f;
            if (tick % 600 == 0) triggerStaticFlicker(3);
        }

        if (currentTier == 0 && !inManhunt) return;

        long timeSinceEffect = tick - lastEffectTick;

        if (currentTier >= 1 && timeSinceEffect > 400 && RNG.nextInt(100) == 0) {
            triggerVignettePulse(0.3f);
            lastEffectTick = tick;
        }

        if (currentTier >= 2 && timeSinceEffect > 200 && RNG.nextInt(150) == 0) {
            triggerStaticFlicker(2 + RNG.nextInt(3));
            triggerVignettePulse(0.5f);
            lastEffectTick = tick;
        }

        if (currentTier >= 3) {
            if (vignetteAlpha < 0.4f) vignetteAlpha = 0.4f;
            if (timeSinceEffect > 100 && RNG.nextInt(300) == 0) {
                watchedOverlayTimer = 60;
                triggerStaticFlicker(1);
                lastEffectTick = tick;
            }
            checkNearbyObsessed(player);
        }
    }

    private static void checkNearbyObsessed(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.entitiesForRendering().forEach(entity -> {
            if (entity.getType().toString().contains("the_obsessed") ||
                entity.getType().toString().contains("the_only_one")) {
                double dist = entity.distanceTo(player);
                if (dist < 20) {
                    float intensity = (float)(1.0 - (dist / 20.0));
                    triggerShake(3, intensity * 0.3f);
                }
            }
        });
    }

    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        if (currentTier == 0 && !inManhunt) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        if (invertFrames > 0) {
            renderInvert(graphics, w, h);
            return;
        }
        if (vignetteAlpha > 0.01f) renderVignette(graphics, w, h, vignetteAlpha);
        if (staticFramesRemaining > 0) renderStatic(graphics, w, h);
        if (watchedOverlayTimer > 0 && currentTier >= 3) renderWatchedText(graphics, w, h);
        if (inManhunt) renderManhuntBorder(graphics, w, h);
    }

    private static void renderVignette(GuiGraphics graphics, int w, int h, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int a = (int)(alpha * 255);
        graphics.fill(0, 0, w, (int)(h * 0.18f), (a << 24));
        graphics.fill(0, (int)(h * 0.82f), w, h, (a << 24));
        graphics.fill(0, 0, (int)(w * 0.12f), h, (a << 24));
        graphics.fill((int)(w * 0.88f), 0, w, h, (a << 24));
        RenderSystem.disableBlend();
    }

    private static void renderStatic(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        for (int i = 0; i < 250; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int gray = 80 + RNG.nextInt(175);
            int alpha = 20 + RNG.nextInt(80);
            graphics.fill(x, y, x + 1, y + 1, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
        RenderSystem.disableBlend();
    }

    private static void renderWatchedText(GuiGraphics graphics, int w, int h) {
        float fade = watchedOverlayTimer / 60.0f;
        int alpha = (int)(fade * 60);
        if (alpha > 5) {
            String text = "it sees you";
            int textWidth = Minecraft.getInstance().font.width(text);
            int color = (alpha << 24) | 0xAAAAAA;
            graphics.drawString(Minecraft.getInstance().font, text,
                w - textWidth - 10, h - 20, color, false);
        }
    }

    private static void renderManhuntBorder(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        int pulse = (int)(Math.abs(Math.sin(System.currentTimeMillis() * 0.002)) * 80) + 20;
        int color = (pulse << 24) | 0x660000;
        int thickness = 6;
        graphics.fill(0, 0, w, thickness, color);
        graphics.fill(0, h - thickness, w, h, color);
        graphics.fill(0, 0, thickness, h, color);
        graphics.fill(w - thickness, 0, w, h, color);
        RenderSystem.disableBlend();
    }

    private static void renderInvert(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        graphics.fill(0, 0, w, h, 0xFFFFFFFF);
        RenderSystem.disableBlend();
    }

    public static float getCameraShakeX() {
        if (shakeFramesRemaining <= 0) return 0f;
        return (RNG.nextFloat() - 0.5f) * shakeIntensity * 2f;
    }

    public static float getCameraShakeY() {
        if (shakeFramesRemaining <= 0) return 0f;
        return (RNG.nextFloat() - 0.5f) * shakeIntensity;
    }

    public static void triggerVignettePulse(float intensity) {
        vignetteAlpha = Math.min(1f, vignetteAlpha + intensity);
    }

    public static void triggerStaticFlicker(int frames) {
        staticFramesRemaining = frames;
    }

    public static void triggerShake(int frames, float intensity) {
        shakeFramesRemaining = frames;
        shakeIntensity = intensity;
    }

    public static void triggerInvert() {
        invertFrames = 2;
    }

    public static void setTier(int tier) {
        currentTier = tier;
    }

    public static void setManhunt(boolean manhunt) {
        inManhunt = manhunt;
    }
}