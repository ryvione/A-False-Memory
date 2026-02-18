package com.ryvione.falsememory.journal.books;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PredictionModelBook {

    public static ItemStack generate(PlayerMemory memory) {
        String page1 = "PREDICTIVE MODEL\n\n" +
            "Pattern confidence: 94.7%\n\n" +
            "Analysis period:\nDay 1 - Day " + memory.worldDayCount + "\n\n" +
            "Subject follows routines with high consistency.\n\n" +
            "Behavioral prediction accuracy has increased from 67% (Day 3) to 94.7%.";

        String page2 = "PREDICTED BEHAVIORS\n\n" +
            "- Will check inventory every " + (memory.averageInventoryCheckInterval / 20) + "s\n" +
            "- Will mine at dusk\n" +
            "- Will sleep at " + getBedCoords(memory) + "\n" +
            "- Will flee below " + String.format("%.1f", memory.averageHealthWhenFleeing) + " hp\n\n" +
            "Subject is highly predictable.";

        return BookUtil.createWrittenBook("Predictive Model", "???", List.of(page1, page2));
    }

    private static String getBedCoords(PlayerMemory memory) {
        if (memory.preferredBedPos == null) return "unknown";
        return memory.preferredBedPos.getX() + ", " + memory.preferredBedPos.getZ();
    }
}