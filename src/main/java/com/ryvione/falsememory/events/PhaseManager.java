package com.ryvione.falsememory.events;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.server.level.ServerPlayer;

public class PhaseManager {

    public static int getPhase(PlayerMemory memory) {
        long day = memory.worldDayCount;
        int tier = memory.knowledgeTier;
        int grace = com.ryvione.falsememory.Config.INSTANCE.gracePeriodDays.get();

        if (day < grace) return 0;
        if (day < grace + 3 || tier < 1) return 1;
        if (day < grace + 7 || tier < 2) return 2;
        if (day < grace + 11 || tier < 3) return 3;
        return 4;
    }

    public static boolean isAtLeastPhase(PlayerMemory memory, int minPhase) {
        return getPhase(memory) >= minPhase;
    }

    public static String getPhaseName(int phase) {
        return switch (phase) {
            case 0 -> "Silence";
            case 1 -> "Noticing";
            case 2 -> "Knowing";
            case 3 -> "Haunting";
            case 4 -> "Confrontation";
            default -> "Unknown";
        };
    }
}