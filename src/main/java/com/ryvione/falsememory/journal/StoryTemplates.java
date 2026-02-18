package com.ryvione.falsememory.journal;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.DirectionUtil;

public class StoryTemplates {

    public static String campDay1(PlayerMemory memory) {
        String biome = memory.visitedBiomes.isEmpty() ? "forest"
            : memory.visitedBiomes.iterator().next().replace("minecraft:", "").replace("_", " ");
        int logCount = memory.blockBreakCounts.getOrDefault("minecraft:oak_log", 47);
        String coords = memory.firstLoginPos != null
            ? memory.firstLoginPos.getX() + ", " + memory.firstLoginPos.getZ() : "unknown";

        return "Day 1 - A New Beginning\n\n" +
            "I spawned in a " + biome + " biome. The sun was setting.\n" +
            "I gathered " + logCount + " oak logs and crafted my first tools.\n" +
            "I built a small shelter near " + coords + ".\n\n" +
            "I felt watched.\nProbably just paranoia.";
    }

    public static String campDay2(PlayerMemory memory) {
        String deathCause = memory.lastDeathCause != null
            ? memory.lastDeathCause.replace("_", " ") : "a creeper";
        String deathCoords = memory.deathPositions.isEmpty() ? "nearby"
            : memory.deathPositions.get(0).getX() + ", " + memory.deathPositions.get(0).getZ();

        return "Day 2 - Settling In\n\n" +
            "I died today. " + deathCause + " caught me off guard near " + deathCoords + ".\n\n" +
            "I heard footsteps when I was alone.\nMy own footsteps.\nBut delayed.\nLike an echo.";
    }

    public static String campDay3(PlayerMemory memory) {
        return "Day 3 - Something's Wrong\n\n" +
            "My crafting table moved. I put it on the left side of my shelter.\nNow it's on the right.\n\n" +
            "Did I move it?\nI don't remember moving it.\n\n" +
            "I checked my inventory.\nMy torches were reorganized.\n" +
            "I always keep them in slot 7.\nNow they're in slot 4.";
    }

    public static String campDay4(PlayerMemory memory) {
        return "Day 4 - I'm Not Alone\n\n" +
            "I saw someone.\nThey looked exactly like me.\n" +
            "Standing 60 blocks away, just... staring.\n\n" +
            "When I ran toward them, they vanished.";
    }

    public static String campDay5Empty() {
        return "Day 5 -\n\n\n\n\n\n[The rest of the page is blank]";
    }

    public static String mineDay6(PlayerMemory memory) {
        String deathCoords = memory.deathPositions.isEmpty() ? "unknown"
            : memory.deathPositions.get(0).getX() + ", " + memory.deathPositions.get(0).getZ();

        return "Day 6 - Going Deeper\n\n" +
            "The tunnel felt familiar.\nLike I'd been here before.\n\n" +
            "I heard mining sounds behind me.\nStone breaking.\nI was alone in the tunnel.";
    }

    public static String mineDay8(PlayerMemory memory) {
        return "Day 8 - It Knows My Routine\n\n" +
            "I went mining at dusk, like I always do.\nWhen I reached my usual spot,\n" +
            "someone was already there.\n\n" +
            "They didn't attack.\nJust mined one block.\nThen walked past me and left.";
    }
}