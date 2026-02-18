package com.ryvione.falsememory.journal.entries;

import com.ryvione.falsememory.memory.PlayerMemory;

public class CombatEntryGenerator {

    public static String combatAnalysisSummary(PlayerMemory memory) {
        return "COMBAT ANALYSIS\n\n" +
            "Weapon: " + memory.preferredWeaponType + "\n" +
            "Combat events: " + memory.totalCombatEvents + "\n" +
            "Flee rate: " + String.format("%.0f%%", memory.getCombatFleeRate() * 100) + "\n" +
            "Flees when: " + String.format("%.1f", memory.averageHealthWhenFleeing) + " hp\n\n" +
            "Uses potions: " + (memory.usesPotions ? "yes" : "no") + "\n" +
            "Uses ranged: " + (memory.usesRangedWeapons ? "yes" : "no") + "\n\n" +
            "I know when you'll run.\nI know how you fight.\nI know your limits.";
    }

    public static String predictionEntry(PlayerMemory memory) {
        return "PREDICTION\n\n" +
            "Pattern confidence: 94.7%\n\n" +
            "Next session:\n" +
            "- You will mine at dusk\n" +
            "- You will check your inventory every " +
            (memory.averageInventoryCheckInterval / 20) + " seconds\n" +
            "- You will sleep at: " + getBedCoords(memory) + "\n\n" +
            "Subject is highly predictable.";
    }

    private static String getBedCoords(PlayerMemory memory) {
        if (memory.preferredBedPos == null) return "unknown";
        return memory.preferredBedPos.getX() + ", " + memory.preferredBedPos.getZ();
    }
}