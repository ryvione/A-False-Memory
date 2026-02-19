package com.ryvione.falsememory.ai.intel;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.CombatAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PlayerIntelligence {

    public float fleeHealthThreshold    = 0.35f;   
    public float preferredEngageRange   = 5.0f;    
    public float avgCombatDuration      = 60f;     
    public boolean sprintAttacks        = false;   
    public boolean circleStrafes        = false;   
    public boolean shieldBlocks         = false;
    public boolean usesPotions          = false;
    public boolean panicsAtLowHP        = false;
    public String counterWeapon         = "sword"; 
    public CombatAnalyzer.CombatStrategy bestStrategyAgainstThem = CombatAnalyzer.CombatStrategy.ADAPTIVE;

    public BlockPos predictedNextLoginPos = null;  
    public BlockPos miningHotspot         = null;  
    public int      preferredMiningY      = -54;   
    public BlockPos homePos               = null;  
    public BlockPos bedPos                = null;
    public List<BlockPos> deathSpots      = new ArrayList<>();
    public List<BlockPos> frequentRoutes  = new ArrayList<>(); 
    public float    dominantFacingYaw     = 0f;    

    public long     avgSessionLengthTicks = 0;     
    public int      loginCount            = 0;
    public long     lastLoginTick         = 0;
    public int      playTimeBucket        = 0;     

    public String   primaryWeapon         = "sword";
    public boolean  hasEnchantedGear      = false;
    public int      estimatedArmorTiers   = 0;     
    public List<String> frequentHotbarItems = new ArrayList<>();

    public float    boldness              = 0.5f;  
    public float    caution               = 0.5f;  
    public float    adaptability          = 0.5f;  
    public boolean  tendsToFlee           = false;
    public boolean  buildsDefenses        = false;
    public boolean  trapAware             = false; 
    public int      panicEvents           = 0;

    public int      dataConfidence        = 0;     
    public long     lastUpdatedTick       = 0;

    private CombatAnalyzer combatAnalyzer = null;

    public PlayerIntelligence() {}

    public void update(ServerPlayer player, PlayerMemory memory) {
        lastUpdatedTick = player.level().getGameTime();

        syncCombatKnowledge(memory);
        syncMovementKnowledge(memory);
        syncScheduleKnowledge(memory);
        syncResourceKnowledge(player, memory);
        syncPsychologicalModel(memory);
        recalculateConfidence(memory);
    }

    private void syncCombatKnowledge(PlayerMemory memory) {
        int total = memory.combatsFled + memory.combatsStoodGround;
        if (total > 0) {
            float fleeRate = (float) memory.combatsFled / total;
            tendsToFlee = fleeRate > 0.45f;
            boldness    = 1.0f - fleeRate;
        }

        if (memory.averageHealthWhenFleeing > 0) {
            fleeHealthThreshold = memory.averageHealthWhenFleeing / 20f;
        }

        primaryWeapon = memory.preferredWeaponType != null ? memory.preferredWeaponType : "sword";
        usesPotions   = memory.usesPotions;

        if (combatAnalyzer != null) {
            sprintAttacks  = combatAnalyzer.getSprintAttackCount() > 3;
            circleStrafes  = combatAnalyzer.getCircleStrafesCount() > 3;
            panicsAtLowHP  = combatAnalyzer.getPanicCount() > 2;
            bestStrategyAgainstThem = combatAnalyzer.getSuggestedStrategy();
            counterWeapon  = combatAnalyzer.getSuggestedWeapon();
        }
    }

    private void syncMovementKnowledge(PlayerMemory memory) {
        homePos = memory.inferredHomePos;
        bedPos  = memory.preferredBedPos;

        deathSpots.clear();
        deathSpots.addAll(memory.deathPositions);

        dominantFacingYaw = memory.getDominantFacingYaw();

        preferredMiningY = memory.getMostCommonMiningY();

        miningHotspot = memory.heatmap.getHotSpot();

        if (memory.loginPositions.size() >= 2) {
            int count = Math.min(5, memory.loginPositions.size());
            int startIdx = memory.loginPositions.size() - count;
            long sx = 0, sy = 0, sz = 0;
            for (int i = startIdx; i < memory.loginPositions.size(); i++) {
                BlockPos lp = memory.loginPositions.get(i);
                sx += lp.getX(); sy += lp.getY(); sz += lp.getZ();
            }
            predictedNextLoginPos = new BlockPos(
                (int)(sx / count), (int)(sy / count), (int)(sz / count));
        }

        frequentRoutes.clear();
        frequentRoutes.addAll(memory.heatmap.getTopLocations(6));
    }

    private void syncScheduleKnowledge(PlayerMemory memory) {
        loginCount = memory.loginPositions.size();
        if (memory.totalTicksPlayed > 0 && loginCount > 0) {
            avgSessionLengthTicks = memory.totalTicksPlayed / Math.max(1, loginCount);
        }
        if      (avgSessionLengthTicks < 6000)  playTimeBucket = 0; 
        else if (avgSessionLengthTicks < 24000) playTimeBucket = 1; 
        else                                     playTimeBucket = 2; 
    }

    private void syncResourceKnowledge(ServerPlayer player, PlayerMemory memory) {
        frequentHotbarItems.clear();
        frequentHotbarItems.addAll(memory.lastHotbarSnapshot);

        int ironBreaks    = memory.blockBreakCounts.getOrDefault("minecraft:iron_ore", 0)
                          + memory.blockBreakCounts.getOrDefault("minecraft:deepslate_iron_ore", 0);
        int diamondBreaks = memory.blockBreakCounts.getOrDefault("minecraft:diamond_ore", 0)
                          + memory.blockBreakCounts.getOrDefault("minecraft:deepslate_diamond_ore", 0);

        if      (diamondBreaks > 20) estimatedArmorTiers = 4;
        else if (diamondBreaks > 5)  estimatedArmorTiers = 3;
        else if (ironBreaks > 30)    estimatedArmorTiers = 2;
        else if (ironBreaks > 5)     estimatedArmorTiers = 1;
        else                         estimatedArmorTiers = 0;

        hasEnchantedGear = estimatedArmorTiers >= 3
            && memory.craftedItems.containsKey("minecraft:enchanting_table");
    }

    private void syncPsychologicalModel(PlayerMemory memory) {
        int totalBlocks = memory.blockPlacementCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalMined  = memory.blockBreakCounts.values().stream().mapToInt(Integer::intValue).sum();
        buildsDefenses  = totalBlocks > 200 && memory.blockPlacementCounts
            .getOrDefault("minecraft:cobblestone", 0) > 20;

        caution = Math.min(1f, memory.totalInventoryChecks / 500f);

        adaptability = Math.min(1f, memory.visitedBiomes.size() / 15f);

        trapAware = memory.blockPlacementCounts.getOrDefault("minecraft:stone_pressure_plate", 0) > 2
            || memory.blockPlacementCounts.getOrDefault("minecraft:oak_pressure_plate", 0) > 2;
    }

    private void recalculateConfidence(PlayerMemory memory) {
        int score = 0;
        if (memory.totalCombatEvents >= 3)    score += 15;
        if (memory.loginPositions.size() >= 3) score += 10;
        if (memory.inferredHomePos != null)    score += 15;
        if (!memory.weaponUseCounts.isEmpty()) score += 10;
        if (memory.totalTicksPlayed > 12000)   score += 10;
        if (!memory.deathPositions.isEmpty())  score += 10;
        if (memory.visitedBiomes.size() > 3)   score += 10;
        if (!memory.chatHistory.isEmpty())      score += 5;
        if (memory.usesPotions)                score += 5;
        if (estimatedArmorTiers > 0)           score += 10;
        dataConfidence = Math.min(100, score);
    }

    public void attachCombatAnalyzer(CombatAnalyzer analyzer) {
        this.combatAnalyzer = analyzer;
    }

    public CombatAnalyzer getCombatAnalyzer() {
        return combatAnalyzer;
    }

    public BlockPos predictDestination(Vec3 playerPos, Vec3 playerVel) {
        
        if (playerVel.length() > 0.15) {
            return BlockPos.containing(
                playerPos.x + playerVel.x * 40,
                playerPos.y + playerVel.y * 10,
                playerPos.z + playerVel.z * 40
            );
        }
        
        if (homePos != null) return homePos;
        if (!frequentRoutes.isEmpty()) return frequentRoutes.get(0);
        return BlockPos.containing(playerPos);
    }

    public BlockPos getBestAmbushPosition(Vec3 myPos, Vec3 playerPos, Vec3 playerVel) {
        Vec3 dest = Vec3.atCenterOf(predictDestination(playerPos, playerVel));
        
        Vec3 intercept = playerPos.lerp(dest, 0.6);
        
        Vec3 perp = new Vec3(-(playerVel.z), 0, playerVel.x).normalize().scale(4);
        return BlockPos.containing(intercept.add(perp));
    }

    public BlockPos getMostRecentDeathSpot() {
        if (deathSpots.isEmpty()) return null;
        return deathSpots.get(deathSpots.size() - 1);
    }

    public boolean predictingFlee(float playerCurrentHP, float playerMaxHP) {
        float ratio = playerCurrentHP / playerMaxHP;
        return ratio <= (fleeHealthThreshold + 0.05f);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fleeHP",           fleeHealthThreshold);
        tag.putFloat("engageRange",      preferredEngageRange);
        tag.putFloat("boldness",         boldness);
        tag.putFloat("caution",          caution);
        tag.putFloat("adaptability",     adaptability);
        tag.putBoolean("tendsToFlee",    tendsToFlee);
        tag.putBoolean("sprintAttacks",  sprintAttacks);
        tag.putBoolean("circleStrafes",  circleStrafes);
        tag.putBoolean("shieldBlocks",   shieldBlocks);
        tag.putBoolean("usesPotions",    usesPotions);
        tag.putBoolean("panicsAtLowHP",  panicsAtLowHP);
        tag.putBoolean("buildsDefenses", buildsDefenses);
        tag.putBoolean("trapAware",      trapAware);
        tag.putString("primaryWeapon",   primaryWeapon);
        tag.putString("counterWeapon",   counterWeapon);
        tag.putInt("estimatedArmor",     estimatedArmorTiers);
        tag.putInt("miningY",            preferredMiningY);
        tag.putInt("dataConfidence",     dataConfidence);
        tag.putInt("panicEvents",        panicEvents);
        tag.putFloat("dominantYaw",      dominantFacingYaw);
        tag.putString("strategy",        bestStrategyAgainstThem.name());
        if (homePos != null) { tag.putInt("hx", homePos.getX()); tag.putInt("hy", homePos.getY()); tag.putInt("hz", homePos.getZ()); }
        if (miningHotspot != null) { tag.putInt("mx", miningHotspot.getX()); tag.putInt("my2", miningHotspot.getY()); tag.putInt("mz", miningHotspot.getZ()); }
        if (predictedNextLoginPos != null) { tag.putInt("lx", predictedNextLoginPos.getX()); tag.putInt("ly", predictedNextLoginPos.getY()); tag.putInt("lz", predictedNextLoginPos.getZ()); }
        return tag;
    }

    public void load(CompoundTag tag) {
        fleeHealthThreshold  = tag.getFloat("fleeHP");
        preferredEngageRange = tag.getFloat("engageRange");
        boldness             = tag.getFloat("boldness");
        caution              = tag.getFloat("caution");
        adaptability         = tag.getFloat("adaptability");
        tendsToFlee          = tag.getBoolean("tendsToFlee");
        sprintAttacks        = tag.getBoolean("sprintAttacks");
        circleStrafes        = tag.getBoolean("circleStrafes");
        shieldBlocks         = tag.getBoolean("shieldBlocks");
        usesPotions          = tag.getBoolean("usesPotions");
        panicsAtLowHP        = tag.getBoolean("panicsAtLowHP");
        buildsDefenses       = tag.getBoolean("buildsDefenses");
        trapAware            = tag.getBoolean("trapAware");
        primaryWeapon        = tag.contains("primaryWeapon") ? tag.getString("primaryWeapon") : "sword";
        counterWeapon        = tag.contains("counterWeapon") ? tag.getString("counterWeapon") : "sword";
        estimatedArmorTiers  = tag.getInt("estimatedArmor");
        preferredMiningY     = tag.getInt("miningY");
        dataConfidence       = tag.getInt("dataConfidence");
        panicEvents          = tag.getInt("panicEvents");
        dominantFacingYaw    = tag.getFloat("dominantYaw");
        try { bestStrategyAgainstThem = CombatAnalyzer.CombatStrategy.valueOf(tag.getString("strategy")); }
        catch (Exception ignored) {}
        if (tag.contains("hx")) homePos = new BlockPos(tag.getInt("hx"), tag.getInt("hy"), tag.getInt("hz"));
        if (tag.contains("mx")) miningHotspot = new BlockPos(tag.getInt("mx"), tag.getInt("my2"), tag.getInt("mz"));
        if (tag.contains("lx")) predictedNextLoginPos = new BlockPos(tag.getInt("lx"), tag.getInt("ly"), tag.getInt("lz"));
    }
}
