package com.ryvione.falsememory.journal.books;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SummoningBook {

    public static ItemStack generate(PlayerMemory memory) {
        String page1 = "When You're Ready\n\n" +
            "Build the altar at your home.\n\n" +
            "3x3 pattern:\n" +
            "- Center: Lectern\n" +
            "  (Book titled YOUR name)\n" +
            "- Corners: Diamond blocks\n" +
            "- Edges: Stone from\n  your death locations";

        String page2 = "Stand at the center.\nRight-click the lectern.\n\n" +
            "\"This is the only way out.\nConfront yourself.\nDefeat yourself.\nOr become yourself.\"\n\n" +
            "There is no escape.\nThe only way out is through.";

        return BookUtil.createWrittenBook("When You're Ready", "???", List.of(page1, page2));
    }
}