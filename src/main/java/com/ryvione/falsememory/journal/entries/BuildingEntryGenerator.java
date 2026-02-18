package com.ryvione.falsememory.journal.entries;

import com.ryvione.falsememory.memory.PlayerMemory;

public class BuildingEntryGenerator {

    public static String fortifyingEntry(PlayerMemory memory) {
        return "Day 11 - Fortifying\n\n" +
            "I built a safehouse. Stone brick walls, iron door.\n" +
            "I organized my chests by category.\n\n" +
            "I feel safer now.\nNothing can get in.";
    }

    public static String itsInsideEntry(PlayerMemory memory) {
        return "Day 12 - It's Inside\n\n" +
            "I came home today.\nThe iron door was open.\n" +
            "I know I closed it.\nI ALWAYS close it.\n\n" +
            "My chests were reorganized.\nNot random — better.\n" +
            "More efficient than my organization.\n\nHow did it know?";
    }

    public static String livesHereNowEntry(PlayerMemory memory) {
        return "Day 13 - It Lives Here Now\n\n" +
            "I saw them in my base today.\nIn MY house.\n" +
            "Standing at my crafting table.\nCrafting a sword.\n\n" +
            "They turned and looked at me.\nThen went back to crafting.\n\n" +
            "Like I wasn't there.\nLike this was THEIR house.";
    }

    public static String whoseHouseEntry(PlayerMemory memory) {
        return "Day 14 - Whose House Is This?\n\n" +
            "I tried to sleep in my bed.\nSomeone was already in it.\n\n" +
            "I tried to use my crafting table.\nSomeone was already using it.\n\n" +
            "Am I the guest in my own home?";
    }

    public static String cantLeaveEntry() {
        StringBuilder sb = new StringBuilder("Day 15 - I Can't Leave\n\n");
        for (int i = 0; i < 12; i++) sb.append("I can't leave\n");
        return sb.toString();
    }
}