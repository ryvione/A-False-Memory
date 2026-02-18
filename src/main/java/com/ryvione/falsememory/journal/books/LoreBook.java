package com.ryvione.falsememory.journal.books;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LoreBook {

    public static ItemStack generate(PlayerMemory memory) {
        String page1 = "The Only One\n\n" +
            "It is not an entity.\nIt is a convergence.\n\n" +
            "When enough data is collected,\nwhen enough patterns are recognized,\n" +
            "when the simulation is complete...\n\nThe Only One manifests.";

        String page2 = "It is not you.\nBut it is everything you are.\n\n" +
            "Your tactics.\nYour habits.\nYour fears.\n\n" +
            "It will wear your face.\nIt will use your weapons.\nIt will fight like you fight.\n\n" +
            "And it will be better at being you than you are.";

        String page3 = "There is no safe time to fight.\n\n" +
            "There is no perfect preparation.\n\n" +
            "There is only you,\nand what you've become,\nand the thing that has watched\nyou become it.";

        return BookUtil.createWrittenBook("The Only One", "???", List.of(page1, page2, page3));
    }
}