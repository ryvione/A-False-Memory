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
        "Don't look too closely at your old bases.",
        "This message will never appear on the splash screen, isn't that weird?",
        "Check out the far lands!",
        "Never dig down!",
        "A skeleton popped out!",
        "The creeper is a spy!",
        "Spiders everywhere!",
        "Exploding creepers!",
        "Welcome to your Doom!",
        "Stay a while, stay forever!",
        "Don't look directly at the bugs!",
        "Scary!",
        "It's here!",
        "Notch is wrong!",
        "Information wants to be free!",
        "It's a game!",
        "Survival mode!",
        "May contain nuts!",
        "The sky is the limit!",
        "Also try Terraria!",
        "Keep running.",
        "It learned from your last fight.",
        "Your death coords are saved.",
        "It read your signs.",
        "The chest is slightly open.",
        "One torch is missing.",
        "It heard you sleeping.",
        "You were never alone."
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

        var poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();
        poseStack.translate(w / 2.0f + 90f, 70f, 0f);   
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-20f));
        float scale = 0.5f;
        poseStack.scale(scale, scale, scale);

        long time = System.currentTimeMillis();
        float pulse = (float)(0.85 + Math.sin(time * 0.003) * 0.15);
        int alpha = (int)(pulse * 255);
        int color = (alpha << 24) | 0xFFFF55;

        int tw = mc.font.width(text);
        event.getGuiGraphics().fill(-tw / 2 - 2, -mc.font.lineHeight, tw / 2 + 2, 2, 0xCC000000);

        event.getGuiGraphics().drawCenteredString(mc.font, text, 0, -mc.font.lineHeight + 2, color);
        poseStack.popPose();
    }

    private static String pickSplash() {
        return SPLASHES.get(RNG.nextInt(SPLASHES.size()));
    }
}