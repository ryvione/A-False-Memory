package com.ryvione.falsememory.journal;

import com.ryvione.falsememory.journal.entries.*;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class JournalGenerator {

    public static ItemStack generateAbandonedCampJournal(PlayerMemory memory) {
        List<String> pages = new ArrayList<>();
        pages.add(StoryTemplates.campDay1(memory));
        pages.add(StoryTemplates.campDay2(memory));
        pages.add(HorrorEntryGenerator.echoEntry(memory));
        pages.add(StoryTemplates.campDay3(memory));
        pages.add(StoryTemplates.campDay4(memory));
        pages.add(StoryTemplates.campDay5Empty());
        return BookUtil.createWrittenBook("Journal", "???", pages);
    }

    public static ItemStack generateMiningOutpostJournal(PlayerMemory memory) {
        List<String> pages = new ArrayList<>();
        pages.add(StoryTemplates.mineDay6(memory));
        pages.add(MiningEntryGenerator.mimicTunnel(memory));
        pages.add(StoryTemplates.mineDay8(memory));
        pages.add(MiningEntryGenerator.cantMineAnymore(memory));
        pages.add(DeathEntryGenerator.bloodStainPage());
        return BookUtil.createWrittenBook("Journal", "???", pages);
    }

    public static ItemStack generateSafehouseJournal(PlayerMemory memory) {
        List<String> pages = new ArrayList<>();
        pages.add(BuildingEntryGenerator.fortifyingEntry(memory));
        pages.add(BuildingEntryGenerator.itsInsideEntry(memory));
        pages.add(BuildingEntryGenerator.livesHereNowEntry(memory));
        pages.add(BuildingEntryGenerator.whoseHouseEntry(memory));
        pages.add(BuildingEntryGenerator.cantLeaveEntry());
        return BookUtil.createWrittenBook("Journal", "???", pages);
    }

    public static ItemStack generateArchiveMainJournal(PlayerMemory memory) {
        List<String> pages = new ArrayList<>();
        pages.add(HorrorEntryGenerator.knowsWhatItIs(memory));
        pages.add(HorrorEntryGenerator.thePattern(memory));
        pages.add(HorrorEntryGenerator.foundTheName());
        pages.add(HorrorEntryGenerator.summoningInstructions());
        pages.add(HorrorEntryGenerator.imReady(memory));
        pages.add(HorrorEntryGenerator.ifYouAreReadingThis());
        return BookUtil.createWrittenBook("The Truth", "???", pages);
    }
}