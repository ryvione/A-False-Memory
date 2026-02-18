package com.ryvione.falsememory.journal.entries;

import com.ryvione.falsememory.memory.PlayerMemory;

import java.util.Random;

public class HorrorEntryGenerator {

    private static final Random RNG = new Random();

    public static String echoEntry(PlayerMemory memory) {
        String chat = memory.chatHistory.isEmpty() ? "Hello?"
            : memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
        return "Day 2.5 - The Echo\n\n" +
            "I said something in chat earlier:\n\"" + chat + "\"\n\n" +
            "An hour later, I heard it again.\nMy own voice.\nFrom behind me.\n\nBut I was alone.";
    }

    public static String knowsWhatItIs(PlayerMemory memory) {
        return "Day 20 - I Know What It Is\n\n" +
            "I found this place.\nThis library.\n\n" +
            "There are books here. Hundreds of them.\n\n" +
            "Each one contains information about me.\n\n" +
            "\"You have died " + memory.totalDeaths + " times\"\n" +
            "\"You prefer " + memory.getMostPlacedBlock().replace("minecraft:", "").replace("_", " ") + "\"\n" +
            "\"You sleep facing " + getBedDirection(memory) + "\"\n\n" +
            "It's been watching since Day 1.";
    }

    public static String thePattern(PlayerMemory memory) {
        return "Day 21 - The Pattern\n\n" +
            "I found a book titled \"Behavior Analysis.\"\n\n" +
            "It documented every fight I've had.\n" +
            "How long each fight lasted.\nWhether I fled or fought.\n\n" +
            "There's another book: \"Predictive Model.\"\n\n" +
            "I read the prediction for today:\n" +
            "\"Subject will read this book.\"\n\"Subject will panic.\"\n\n" +
            "I checked the time.";
    }

    public static String foundTheName() {
        return "Day 22 - I Found The Name\n\n" +
            "There's a book on the center lectern.\n\n" +
            "The title: \"The Only One\"\n\n" +
            "\"It is not you.\nBut it is everything you are.\n\n" +
            "Your tactics. Your habits. Your fears.\n\n" +
            "It will wear your face.\nIt will fight like you fight.\n\n" +
            "And it will be better at being you than you are.\"";
    }

    public static String summoningInstructions() {
        return "Day 23 - When You're Ready\n\n" +
            "Build a 3x3 altar.\n\n" +
            "Center: A lectern with a written book titled with YOUR name.\n\n" +
            "Corners: Diamond blocks.\n\n" +
            "Edges: Blocks from your death locations.\n\n" +
            "Stand at the center.\nRight-click the book.\n\n" +
            "\"This is the only way out.\nConfront yourself.\nDefeat yourself.\"";
    }

    public static String imReady(PlayerMemory memory) {
        return "Day 25 - I'm Ready\n\n" +
            "I built the altar.\nI gathered the items.\n" +
            "I wrote my name in the final book.\n\n" +
            "I placed the book on the altar.\n\nThe ground shook.\n\nIt's here.";
    }

    public static String ifYouAreReadingThis() {
        return "Day 27 - If You're Reading This\n\n" +
            "You can't run.\nYou can't hide.\nYou can only fight.\n\n" +
            "When you're ready, build the altar.\nFace The Only One.\n\n" +
            "Maybe you'll win.\nMaybe I'll see you on the other side.\n\n" +
            "Or maybe...\nMaybe you're me.\nMaybe I'm you.\n\n" +
            "Good luck.\n\n§8— ???";
    }

    private static String getBedDirection(PlayerMemory memory) {
        if (memory.preferredBedPos == null) return "somewhere";
        return "x=" + memory.preferredBedPos.getX();
    }
}