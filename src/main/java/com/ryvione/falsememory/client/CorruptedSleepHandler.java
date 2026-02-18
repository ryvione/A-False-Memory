package com.ryvione.falsememory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CorruptedSleepHandler {

    private static boolean wasSleeping = false;
    private static int flashFrames = 0;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean isSleeping = player.isSleeping();

        if (!wasSleeping && isSleeping) {
            int tier = SanityEffects.getCurrentTier();
            if (tier >= 2) {
                flashFrames = 3;
                SanityEffects.triggerStaticFlicker(3);
                SanityEffects.triggerVignettePulse(0.9f);
            }
        }

        wasSleeping = isSleeping;

        if (flashFrames > 0) {
            flashFrames--;
            if (flashFrames == 2) {
                SanityEffects.triggerInvert();
            }
        }
    }
}