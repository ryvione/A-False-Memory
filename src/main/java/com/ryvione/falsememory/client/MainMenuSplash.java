package com.ryvione.falsememory.client;

import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import com.ryvione.falsememory.FalseMemory;

import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = FalseMemory.MOD_ID, value = Dist.CLIENT)
public class MainMenuSplash {

    private static final Random RNG = new Random();

    private static final List<String> SPLASHES = List.of(
        "You're not alone.",
        "It knows everything.",
        "It was just here.",
        "Don't look behind you.",
        "It remembers your name.",
        "Your world knows too much.",
        "It wore your face today.",
        "The footsteps weren't yours.",
        "It finished your sentence.",
        "Be careful.",
        "It already knows where you'll spawn.",
        "You always face North.",
        "I was in your base last night.",
        "It counts your deaths.",
        "You check your inventory every 45 seconds.",
        "It knows when you'll run.",
        "Your habits gave you away.",
        "It built a copy of your home.",
        "Something is watching.",
        "Play alone. Play at night.",
        "Don't look too closely at your old bases."
    );

    private static String currentSplash = pickSplash();

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen) {
            currentSplash = pickSplash();
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;

        var mc = net.minecraft.client.Minecraft.getInstance();
        int w = screen.width;
        int h = screen.height;

        String text = currentSplash;
        int textWidth = mc.font.width(text);

        float scale = 1.4f;
        int x = (int)((w / 2f - (textWidth * scale) / 2f));
        int y = (int)(h * 0.42f);

        var poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);

        long time = System.currentTimeMillis();
        float pulse = (float)(0.85 + Math.sin(time * 0.003) * 0.15);
        int alpha = (int)(pulse * 255);
        int color = (alpha << 24) | 0xFFFF55;

        event.getGuiGraphics().drawString(mc.font, text, 0, 0, color, true);
        poseStack.popPose();
    }

    private static String pickSplash() {
        return SPLASHES.get(RNG.nextInt(SPLASHES.size()));
    }
}