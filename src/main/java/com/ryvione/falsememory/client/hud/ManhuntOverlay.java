package com.ryvione.falsememory.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ManhuntOverlay {

    public static void render(GuiGraphics graphics, int w, int h) {
        RenderSystem.enableBlend();
        int pulse = (int)(Math.abs(Math.sin(System.currentTimeMillis() * 0.002)) * 80) + 20;
        int color = (pulse << 24) | 0x660000;
        int t = 6;
        graphics.fill(0, 0, w, t, color);
        graphics.fill(0, h - t, w, h, color);
        graphics.fill(0, 0, t, h, color);
        graphics.fill(w - t, 0, w, h, color);
        RenderSystem.disableBlend();
    }
}