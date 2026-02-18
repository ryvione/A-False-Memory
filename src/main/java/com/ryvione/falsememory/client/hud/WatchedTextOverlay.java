package com.ryvione.falsememory.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WatchedTextOverlay {

    public static void render(GuiGraphics graphics, int w, int h, int watchedTimer) {
        if (watchedTimer <= 0) return;
        float fade = watchedTimer / 60.0f;
        int alpha = (int)(fade * 60);
        if (alpha <= 5) return;

        String text = "it sees you";
        int textWidth = Minecraft.getInstance().font.width(text);
        int color = (alpha << 24) | 0xAAAAAA;
        graphics.drawString(Minecraft.getInstance().font, text,
            w - textWidth - 10, h - 20, color, false);
    }
}