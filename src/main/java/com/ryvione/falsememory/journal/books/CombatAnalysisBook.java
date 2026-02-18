package com.ryvione.falsememory.journal.books;

import com.ryvione.falsememory.journal.entries.CombatEntryGenerator;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CombatAnalysisBook {

    public static ItemStack generate(PlayerMemory memory) {
        return BookUtil.createWrittenBook(
            "Behavior Analysis - Combat",
            "???",
            List.of(
                CombatEntryGenerator.combatAnalysisSummary(memory),
                CombatEntryGenerator.predictionEntry(memory)
            )
        );
    }
}