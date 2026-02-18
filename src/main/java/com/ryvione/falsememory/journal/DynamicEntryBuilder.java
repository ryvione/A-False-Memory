package com.ryvione.falsememory.journal;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.DirectionUtil;

import java.util.Random;

public class DynamicEntryBuilder {

    private static final Random RNG = new Random();

    public static String buildFromMemory(PlayerMemory memory, String template) {
        String result = template;
        result = result.replace("{BIOME}", getBiome(memory));
        result = result.replace("{DIRECTION}", DirectionUtil.getDirectionName(memory.getDominantFacingYaw()));
        result = result.replace("{DEATHS}", String.valueOf(memory.totalDeaths));
        result = result.replace("{DEATH_CAUSE}", getDeathCause(memory));
        result = result.replace("{HOME_X}", getHomeX(memory));
        result = result.replace("{HOME_Z}", getHomeZ(memory));
        result = result.replace("{WEAPON}", memory.preferredWeaponType);
        result = result.replace("{FLEE_HP}", String.format("%.1f", memory.averageHealthWhenFleeing));
        result = result.replace("{MOST_PLACED}", cleanBlockId(memory.getMostPlacedBlock()));
        result = result.replace("{CHAT}", getRandomChat(memory));
        result = result.replace("{INVENTORY_INTERVAL}",
            String.valueOf(memory.averageInventoryCheckInterval / 20));
        result = result.replace("{SLEEP_COUNT}", String.valueOf(memory.sleepCount));
        return result;
    }

    private static String getBiome(PlayerMemory memory) {
        if (memory.visitedBiomes.isEmpty()) return "forest";
        return memory.visitedBiomes.iterator().next().replace("minecraft:", "").replace("_", " ");
    }

    private static String getDeathCause(PlayerMemory memory) {
        if (memory.lastDeathCause == null) return "something";
        return memory.lastDeathCause.replace("_", " ");
    }

    private static String getHomeX(PlayerMemory memory) {
        return memory.inferredHomePos != null ? String.valueOf(memory.inferredHomePos.getX()) : "???";
    }

    private static String getHomeZ(PlayerMemory memory) {
        return memory.inferredHomePos != null ? String.valueOf(memory.inferredHomePos.getZ()) : "???";
    }

    private static String getRandomChat(PlayerMemory memory) {
        if (memory.chatHistory.isEmpty()) return "Hello?";
        return memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
    }

    private static String cleanBlockId(String blockId) {
        return blockId.replace("minecraft:", "").replace("_", " ");
    }
}