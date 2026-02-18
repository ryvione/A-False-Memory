package com.ryvione.falsememory.journal.entries;

import com.ryvione.falsememory.memory.PlayerMemory;

public class MiningEntryGenerator {

    public static String mimicTunnel(PlayerMemory memory) {
        return "Day 7 - The Mimic\n\n" +
            "There's another tunnel connected to mine now.\nI didn't dig it.\n" +
            "But it's in my exact style.\n\n" +
            "I found a sign at the end:\n" +
            "\"You always place torches on the right wall.\"\n\n" +
            "How did they know?";
    }

    public static String cantMineAnymore(PlayerMemory memory) {
        return "Day 9 - I Can't Mine Anymore\n\n" +
            "Every tunnel I dig,\nI find one already there.\n\n" +
            "Every ore I plan to mine is already gone.\n\n" +
            "Every torch I place is already placed.\n\n" +
            "It's always one step ahead of me.";
    }
}