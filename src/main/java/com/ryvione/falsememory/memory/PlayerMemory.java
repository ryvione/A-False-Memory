package com.ryvione.falsememory.memory;

import com.ryvione.falsememory.tracking.TrapAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class PlayerMemory {

    public BlockPos firstLoginPos = null;
    public final List<BlockPos> loginPositions = new ArrayList<>();
    public long totalTicksPlayed = 0;
    public long worldDayCount = 0;

    public final int[] facingBuckets = new int[8];
    public final Map<Long, Integer> chunkVisitCounts = new LinkedHashMap<>();
    public BlockPos inferredHomePos = null;

    public final Map<String, Integer> craftedItems = new LinkedHashMap<>();
    public final List<String> lastHotbarSnapshot = new ArrayList<>();
    public final Map<String, Integer> blockPlacementCounts = new LinkedHashMap<>();
    public final Map<String, Integer> blockBreakCounts = new LinkedHashMap<>();

    public final List<BlockPos> deathPositions = new ArrayList<>();
    public String lastDeathCause = null;
    public int totalDeaths = 0;
    public final Map<String, Integer> deathCauseHistory = new LinkedHashMap<>();

    public final Set<String> visitedBiomes = new LinkedHashSet<>();
    public String mostFrequentBiome = null;

    public final List<String> chatHistory = new ArrayList<>();
    public static final int MAX_CHAT_HISTORY = 20;

    public int sleepCount = 0;
    public BlockPos preferredBedPos = null;

    public final Set<String> triggeredEvents = new LinkedHashSet<>();
    public long lastEventDay = -1;
    public int knowledgeTier = 0;

    public final List<BlockPos> recentlyPlacedBlocks = new ArrayList<>();
    public static final int MAX_RECENT_BLOCKS = 50;

    public String preferredWeaponType = "sword";
    public int combatsFled = 0;
    public int combatsStoodGround = 0;
    public boolean usesRangedWeapons = false;
    public boolean usesPotions = false;
    public float averageHealthWhenFleeing = 8.0f;
    public int totalCombatEvents = 0;
    public long totalCombatTicks = 0;
    public final Map<String, Integer> weaponUseCounts = new LinkedHashMap<>();

    public long lastInventoryCheckTick = 0;
    public long totalInventoryChecks = 0;
    public long averageInventoryCheckInterval = 45;

    public boolean inManhunt = false;
    public long manhuntStartDay = -1;

    public final List<TrapAnalyzer.TrapMechanism> detectedTraps = new ArrayList<>();
    public final Set<String> learnedTrapPatterns = new LinkedHashSet<>();

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag breakTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : blockBreakCounts.entrySet()) breakTag.putInt(e.getKey(), e.getValue());
        
        tag.put("blockBreakCounts", breakTag);
        tag.putLong("totalTicksPlayed", totalTicksPlayed);
        tag.putLong("worldDayCount", worldDayCount);
        tag.putInt("totalDeaths", totalDeaths);
        tag.putInt("sleepCount", sleepCount);
        tag.putInt("knowledgeTier", knowledgeTier);
        tag.putLong("lastEventDay", lastEventDay);
        tag.putString("preferredWeaponType", preferredWeaponType);
        tag.putInt("combatsFled", combatsFled);
        tag.putInt("combatsStoodGround", combatsStoodGround);
        tag.putBoolean("usesRangedWeapons", usesRangedWeapons);
        tag.putBoolean("usesPotions", usesPotions);
        tag.putFloat("averageHealthWhenFleeing", averageHealthWhenFleeing);
        tag.putInt("totalCombatEvents", totalCombatEvents);
        tag.putLong("totalCombatTicks", totalCombatTicks);
        tag.putLong("totalInventoryChecks", totalInventoryChecks);
        tag.putLong("averageInventoryCheckInterval", averageInventoryCheckInterval);
        tag.putBoolean("inManhunt", inManhunt);
        tag.putLong("manhuntStartDay", manhuntStartDay);

        if (lastDeathCause != null) tag.putString("lastDeathCause", lastDeathCause);
        if (mostFrequentBiome != null) tag.putString("mostFrequentBiome", mostFrequentBiome);

        if (firstLoginPos != null) tag.put("firstLoginPos", writeBlockPos(firstLoginPos));
        if (inferredHomePos != null) tag.put("inferredHomePos", writeBlockPos(inferredHomePos));
        if (preferredBedPos != null) tag.put("preferredBedPos", writeBlockPos(preferredBedPos));

        ListTag loginList = new ListTag();
        int startLogin = Math.max(0, loginPositions.size() - 10);
        for (int i = startLogin; i < loginPositions.size(); i++) loginList.add(writeBlockPos(loginPositions.get(i)));
        tag.put("loginPositions", loginList);

        ListTag deathList = new ListTag();
        for (BlockPos pos : deathPositions) deathList.add(writeBlockPos(pos));
        tag.put("deathPositions", deathList);

        ListTag recentBlocks = new ListTag();
        for (BlockPos pos : recentlyPlacedBlocks) recentBlocks.add(writeBlockPos(pos));
        tag.put("recentlyPlacedBlocks", recentBlocks);

        ListTag biomeList = new ListTag();
        for (String b : visitedBiomes) biomeList.add(StringTag.valueOf(b));
        tag.put("visitedBiomes", biomeList);

        ListTag chatList = new ListTag();
        for (String c : chatHistory) chatList.add(StringTag.valueOf(c));
        tag.put("chatHistory", chatList);

        ListTag eventList = new ListTag();
        for (String e : triggeredEvents) eventList.add(StringTag.valueOf(e));
        tag.put("triggeredEvents", eventList);

        CompoundTag craftTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : craftedItems.entrySet()) craftTag.putInt(e.getKey(), e.getValue());
        tag.put("craftedItems", craftTag);

        CompoundTag blockTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : blockPlacementCounts.entrySet()) blockTag.putInt(e.getKey(), e.getValue());
        tag.put("blockPlacementCounts", blockTag);

        CompoundTag deathCauseTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : deathCauseHistory.entrySet()) deathCauseTag.putInt(e.getKey(), e.getValue());
        tag.put("deathCauseHistory", deathCauseTag);

        CompoundTag weaponTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : weaponUseCounts.entrySet()) weaponTag.putInt(e.getKey(), e.getValue());
        tag.put("weaponUseCounts", weaponTag);

        tag.putIntArray("facingBuckets", facingBuckets);

        CompoundTag chunkTag = new CompoundTag();
        chunkVisitCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(100)
            .forEach(e -> chunkTag.putInt(String.valueOf(e.getKey()), e.getValue()));
        tag.put("chunkVisitCounts", chunkTag);

        ListTag hotbarList = new ListTag();
        for (String s : lastHotbarSnapshot) hotbarList.add(StringTag.valueOf(s));
        tag.put("lastHotbarSnapshot", hotbarList);

        return tag;
    }

    public static PlayerMemory load(CompoundTag tag) {
        PlayerMemory m = new PlayerMemory();
        CompoundTag breakTag = tag.getCompound("blockBreakCounts");
        for (String key : breakTag.getAllKeys()) m.blockBreakCounts.put(key, breakTag.getInt(key));

        m.totalTicksPlayed = tag.getLong("totalTicksPlayed");
        m.worldDayCount = tag.getLong("worldDayCount");
        m.totalDeaths = tag.getInt("totalDeaths");
        m.sleepCount = tag.getInt("sleepCount");
        m.knowledgeTier = tag.getInt("knowledgeTier");
        m.lastEventDay = tag.getLong("lastEventDay");
        m.combatsFled = tag.getInt("combatsFled");
        m.combatsStoodGround = tag.getInt("combatsStoodGround");
        m.usesRangedWeapons = tag.getBoolean("usesRangedWeapons");
        m.usesPotions = tag.getBoolean("usesPotions");
        m.averageHealthWhenFleeing = tag.getFloat("averageHealthWhenFleeing");
        m.totalCombatEvents = tag.getInt("totalCombatEvents");
        m.totalCombatTicks = tag.getLong("totalCombatTicks");
        m.totalInventoryChecks = tag.getLong("totalInventoryChecks");
        m.averageInventoryCheckInterval = tag.getLong("averageInventoryCheckInterval");
        m.inManhunt = tag.getBoolean("inManhunt");
        m.manhuntStartDay = tag.getLong("manhuntStartDay");

        if (tag.contains("preferredWeaponType")) m.preferredWeaponType = tag.getString("preferredWeaponType");
        if (tag.contains("lastDeathCause")) m.lastDeathCause = tag.getString("lastDeathCause");
        if (tag.contains("mostFrequentBiome")) m.mostFrequentBiome = tag.getString("mostFrequentBiome");

        if (tag.contains("firstLoginPos")) m.firstLoginPos = readBlockPos(tag.getCompound("firstLoginPos"));
        if (tag.contains("inferredHomePos")) m.inferredHomePos = readBlockPos(tag.getCompound("inferredHomePos"));
        if (tag.contains("preferredBedPos")) m.preferredBedPos = readBlockPos(tag.getCompound("preferredBedPos"));

        ListTag loginList = tag.getList("loginPositions", Tag.TAG_COMPOUND);
        for (int i = 0; i < loginList.size(); i++) m.loginPositions.add(readBlockPos(loginList.getCompound(i)));

        ListTag deathList = tag.getList("deathPositions", Tag.TAG_COMPOUND);
        for (int i = 0; i < deathList.size(); i++) m.deathPositions.add(readBlockPos(deathList.getCompound(i)));

        ListTag recentBlocks = tag.getList("recentlyPlacedBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < recentBlocks.size(); i++) m.recentlyPlacedBlocks.add(readBlockPos(recentBlocks.getCompound(i)));

        ListTag biomeList = tag.getList("visitedBiomes", Tag.TAG_STRING);
        for (int i = 0; i < biomeList.size(); i++) m.visitedBiomes.add(biomeList.getString(i));

        ListTag chatList = tag.getList("chatHistory", Tag.TAG_STRING);
        for (int i = 0; i < chatList.size(); i++) m.chatHistory.add(chatList.getString(i));

        ListTag eventList = tag.getList("triggeredEvents", Tag.TAG_STRING);
        for (int i = 0; i < eventList.size(); i++) m.triggeredEvents.add(eventList.getString(i));

        CompoundTag craftTag = tag.getCompound("craftedItems");
        for (String key : craftTag.getAllKeys()) m.craftedItems.put(key, craftTag.getInt(key));

        CompoundTag blockTag = tag.getCompound("blockPlacementCounts");
        for (String key : blockTag.getAllKeys()) m.blockPlacementCounts.put(key, blockTag.getInt(key));

        CompoundTag deathCauseTag = tag.getCompound("deathCauseHistory");
        for (String key : deathCauseTag.getAllKeys()) m.deathCauseHistory.put(key, deathCauseTag.getInt(key));

        CompoundTag weaponTag = tag.getCompound("weaponUseCounts");
        for (String key : weaponTag.getAllKeys()) m.weaponUseCounts.put(key, weaponTag.getInt(key));

        if (tag.contains("facingBuckets")) {
            int[] fb = tag.getIntArray("facingBuckets");
            System.arraycopy(fb, 0, m.facingBuckets, 0, Math.min(fb.length, 8));
        }

        CompoundTag chunkTag = tag.getCompound("chunkVisitCounts");
        for (String key : chunkTag.getAllKeys()) {
            try { m.chunkVisitCounts.put(Long.parseLong(key), chunkTag.getInt(key)); } catch (Exception ignored) {}
        }

        ListTag hotbarList = tag.getList("lastHotbarSnapshot", Tag.TAG_STRING);
        for (int i = 0; i < hotbarList.size(); i++) m.lastHotbarSnapshot.add(hotbarList.getString(i));

        return m;
    }

    private static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag t = new CompoundTag();
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        return t;
    }

    private static BlockPos readBlockPos(CompoundTag t) {
        return new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
    }

    public float getDominantFacingYaw() {
        int maxBucket = 0;
        for (int i = 1; i < 8; i++) {
            if (facingBuckets[i] > facingBuckets[maxBucket]) maxBucket = i;
        }
        return maxBucket * 45f;
    }

    public void recordFacing(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        int bucket = (int)(normalized / 45f) % 8;
        facingBuckets[bucket]++;
    }

    public void recordChunkVisit(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        chunkVisitCounts.merge(key, 1, Integer::sum);
        chunkVisitCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(e -> {
                long k = e.getKey();
                int cx = (int)(k >> 32);
                int cz = (int)(k & 0xFFFFFFFFL);
                inferredHomePos = new BlockPos(cx * 16 + 8, 64, cz * 16 + 8);
            });
    }

    public void recordChat(String message) {
        chatHistory.add(message);
        while (chatHistory.size() > MAX_CHAT_HISTORY) chatHistory.remove(0);
    }

    public void recordPlacedBlock(BlockPos pos) {
        recentlyPlacedBlocks.add(pos);
        while (recentlyPlacedBlocks.size() > MAX_RECENT_BLOCKS) recentlyPlacedBlocks.remove(0);
    }

    public String getMostPlacedBlock() {
        return blockPlacementCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("minecraft:cobblestone");
    }

    public String getMostFrequentDeathCause() {
        return deathCauseHistory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public float getCombatFleeRate() {
        int total = combatsFled + combatsStoodGround;
        if (total == 0) return 0.5f;
        return (float) combatsFled / total;
    }

    public boolean tendsToFlee() {
        return getCombatFleeRate() > 0.5f;
    }

    public void recalculateTier() {
        int score = 0;
        if (totalTicksPlayed > 20 * 60 * 20) score++;
        if (totalTicksPlayed > 20 * 60 * 120) score++;
        if (!craftedItems.isEmpty()) score++;
        if (!deathPositions.isEmpty()) score++;
        if (inferredHomePos != null) score++;
        if (!chatHistory.isEmpty()) score++;
        if (preferredBedPos != null) score++;
        if (loginPositions.size() >= 3) score++;
        if (visitedBiomes.size() >= 3) score++;
        if (totalCombatEvents >= 5) score++;
        if (!weaponUseCounts.isEmpty()) score++;

        knowledgeTier = Math.min(3, score / 3);
    }

    public boolean wasTriggeredToday(String eventId) {
        return triggeredEvents.contains(eventId + "_day" + worldDayCount);
    }

    public void markTriggered(String eventId) {
        triggeredEvents.add(eventId + "_day" + worldDayCount);
        lastEventDay = worldDayCount;
    }

    public long getDaysSinceLastEvent() {
        return worldDayCount - lastEventDay;
    }
}