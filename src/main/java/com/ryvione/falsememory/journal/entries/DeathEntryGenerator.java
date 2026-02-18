package com.ryvione.falsememory.journal.entries;

import com.ryvione.falsememory.memory.PlayerMemory;

public class DeathEntryGenerator {

    public static String deathRecollection(PlayerMemory memory) {
        if (memory.deathPositions.isEmpty()) {
            return "I have not died yet.\n\nBut I will.\n\nIt knows how.";
        }

        StringBuilder sb = new StringBuilder("Your Deaths\n\n");
        int max = Math.min(memory.deathPositions.size(), 5);
        for (int i = 0; i < max; i++) {
            var pos = memory.deathPositions.get(i);
            sb.append("#").append(i + 1).append(": ")
                .append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ())
                .append("\n");
        }
        sb.append("\nI was there.\nFor each one.");
        return sb.toString();
    }

    public static String bloodStainPage() {
        return "Day 10 -\n\n\n\n\n[There is a stain on this page]\n\n[You cannot read what was written]";
    }
}