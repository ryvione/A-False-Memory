package com.ryvione.falsememory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CorruptedSleepHandler {

    private static boolean wasSleeping = false;
    private static int flashFrames = 0;
    private static int wakeHorrorFrames = 0;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean isSleeping = player.isSleeping();
        int tier = SanityEffects.getCurrentTier();

        if (!wasSleeping && isSleeping && tier >= 2) {
            flashFrames = 6;
            SanityEffects.triggerStaticFlicker(6);
            SanityEffects.triggerVignettePulse(1.0f);
            SanityEffects.triggerShake(4, 0.4f);
        }

        if (wasSleeping && !isSleeping && tier >= 1) {
            wakeHorrorFrames = 80;
            SanityEffects.triggerVignettePulse(0.8f);
            SanityEffects.triggerStaticFlicker(5);
            SanityEffects.triggerShake(8, 0.3f);
        }

        wasSleeping = isSleeping;

        if (flashFrames > 0) {
            flashFrames--;
            if (flashFrames == 4) SanityEffects.triggerInvert();
            if (flashFrames == 2) SanityEffects.triggerInvert();
        }

        if (wakeHorrorFrames > 0) {
            wakeHorrorFrames--;
            if (wakeHorrorFrames % 20 == 0 && tier >= 2) {
                SanityEffects.triggerStaticFlicker(2);
                SanityEffects.triggerVignettePulse(0.3f);
            }
        }
    }
}
