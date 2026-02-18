package com.ryvione.falsememory.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class SanityOverlay {

    private static final Random RNG = new Random();

    public static void renderVignette(GuiGraphics graphics, int w, int h, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int a = (int)(alpha * 255);
        graphics.fill(0, 0, w, (int)(h * 0.18f), (a << 24));
        graphics.fill(0, (int)(h * 0.82f), w, h, (a << 24));
        graphics.fill(0, 0, (int)(w * 0.12f), h, (a << 24));
        graphics.fill((int)(w * 0.88f), 0, w, h, (a << 24));
        RenderSystem.disableBlend();
    }

    public static void renderStatic(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        for (int i = 0; i < 250; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int gray = 80 + RNG.nextInt(175);
            int alpha = 20 + RNG.nextInt(80);
            graphics.fill(x, y, x + 1, y + 1,
                (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
        RenderSystem.disableBlend();
    }

    public static void renderInvert(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        graphics.fill(0, 0, w, h, 0xFFFFFFFF);
        RenderSystem.disableBlend();
    }
}