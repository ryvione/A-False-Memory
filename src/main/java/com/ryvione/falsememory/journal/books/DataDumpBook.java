package com.ryvione.falsememory.journal.books;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BookUtil;
import com.ryvione.falsememory.util.DirectionUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DataDumpBook {

    public static ItemStack generate(PlayerMemory memory) {
        String page1 = "SUBJECT DATA\n\n" +
            "Days tracked: " + memory.worldDayCount + "\n" +
            "Deaths: " + memory.totalDeaths + "\n" +
            "Sleep count: " + memory.sleepCount + "\n" +
            "Biomes visited: " + memory.visitedBiomes.size() + "\n" +
            "Items crafted: " + memory.craftedItems.size() + " types\n" +
            "Blocks placed: " + memory.recentlyPlacedBlocks.size() + "\n\n" +
            "Dominant direction: " +
            DirectionUtil.getDirectionName(memory.getDominantFacingYaw());

        String page2 = "BEHAVIORAL PROFILE\n\n" +
            "Weapon: " + memory.preferredWeaponType + "\n" +
            "Flee rate: " + String.format("%.0f%%", memory.getCombatFleeRate() * 100) + "\n" +
            "Flee threshold: " + String.format("%.1f", memory.averageHealthWhenFleeing) + " hp\n" +
            "Uses ranged: " + memory.usesRangedWeapons + "\n" +
            "Uses potions: " + memory.usesPotions + "\n\n" +
            "Most placed: " + memory.getMostPlacedBlock().replace("minecraft:", "") + "\n" +
            "Inventory check: every " + (memory.averageInventoryCheckInterval / 20) + "s";

        return BookUtil.createWrittenBook("Subject Data", "???", List.of(page1, page2));
    }
}