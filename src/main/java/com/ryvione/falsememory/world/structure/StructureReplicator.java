package com.ryvione.falsememory.world.structure;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.TrapAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.*;

public class StructureReplicator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static BlockPos replicatePlayerBase(ServerLevel level, PlayerMemory memory, BlockPos replicaCenter) {
        if (memory.inferredHomePos == null) {
            LOGGER.warn("[FalseMemory] Cannot replicate base: no home position inferred");
            return null;
        }

        BlockPos originalHome = memory.inferredHomePos;
        Map<BlockPos, String> baseBlocks = extractBaseStructure(level, originalHome, memory);
        
        if (baseBlocks.isEmpty()) {
            LOGGER.warn("[FalseMemory] Base structure empty, creating default shelter");
            createDefaultShelter(level, replicaCenter);
            return replicaCenter;
        }

        BlockPos offset = replicaCenter.subtract(originalHome);
        replaceBlocks(level, baseBlocks, offset);
        replicateTraps(level, memory, offset);

        LOGGER.info("[FalseMemory] Replicated base with {} blocks and {} traps",
            baseBlocks.size(), memory.detectedTraps.size());

        return replicaCenter;
    }

    private static Map<BlockPos, String> extractBaseStructure(ServerLevel level, BlockPos center, PlayerMemory memory) {
        Map<BlockPos, String> blocks = new LinkedHashMap<>();
        int radius = 32;
        int height = 16;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    var state = level.getBlockState(pos);
                    
                    if (!state.blocksMotion() && state.getBlock() != Blocks.AIR) {
                        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                        
                        if (memory.blockPlacementCounts.containsKey(blockId) ||
                            memory.blockBreakCounts.containsKey(blockId)) {
                            blocks.put(pos, blockId);
                        }
                    }
                }
            }
        }

        return blocks;
    }

    private static void replaceBlocks(ServerLevel level, Map<BlockPos, String> blocks, BlockPos offset) {
        for (Map.Entry<BlockPos, String> entry : blocks.entrySet()) {
            BlockPos originalPos = entry.getKey();
            BlockPos newPos = originalPos.offset(offset);
            String blockId = entry.getValue();

            try {
                var blockOpt = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockId));
                blockOpt.ifPresent(block -> {
                    level.setBlock(newPos, block.defaultBlockState(), 3);
                });
            } catch (Exception ignored) {}
        }
    }

    private static void replicateTraps(ServerLevel level, PlayerMemory memory, BlockPos offset) {
        for (TrapAnalyzer.TrapMechanism trap : memory.detectedTraps) {
            BlockPos newTriggerPos = trap.triggerPos.offset(offset);

            switch (trap.trapType) {
                case "pressure_plate" -> replicatePressurePlateTrap(level, trap, newTriggerPos);
                case "piston" -> replicatePistonTrap(level, trap, newTriggerPos);
                case "redstone" -> replicateRedstoneCircuit(level, trap, newTriggerPos);
                case "door" -> replicateDoorMechanism(level, trap, newTriggerPos);
            }
        }
    }

    private static void replicatePressurePlateTrap(ServerLevel level, TrapAnalyzer.TrapMechanism trap, BlockPos triggerPos) {
        level.setBlock(triggerPos, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState(), 3);
        
        for (BlockPos affected : trap.affectedBlocks) {
            BlockPos newPos = affected.offset(triggerPos.getX() - trap.triggerPos.getX(),
                                             triggerPos.getY() - trap.triggerPos.getY(),
                                             triggerPos.getZ() - trap.triggerPos.getZ());
            
            if (level.getBlockState(newPos).getBlock() == Blocks.AIR) {
                level.setBlock(newPos, Blocks.STICKY_PISTON.defaultBlockState(), 3);
            }
        }
    }

    private static void replicatePistonTrap(ServerLevel level, TrapAnalyzer.TrapMechanism trap, BlockPos triggerPos) {
        level.setBlock(triggerPos, Blocks.PISTON.defaultBlockState(), 3);
        
        if (!trap.affectedBlocks.isEmpty()) {
            BlockPos affected = trap.affectedBlocks.get(0);
            BlockPos newAffected = affected.offset(triggerPos.getX() - trap.triggerPos.getX(),
                                                   triggerPos.getY() - trap.triggerPos.getY(),
                                                   triggerPos.getZ() - trap.triggerPos.getZ());
            level.setBlock(newAffected, Blocks.SLIME_BLOCK.defaultBlockState(), 3);
        }
    }

    private static void replicateRedstoneCircuit(ServerLevel level, TrapAnalyzer.TrapMechanism trap, BlockPos triggerPos) {
        for (BlockPos affected : trap.affectedBlocks) {
            BlockPos newPos = affected.offset(triggerPos.getX() - trap.triggerPos.getX(),
                                             triggerPos.getY() - trap.triggerPos.getY(),
                                             triggerPos.getZ() - trap.triggerPos.getZ());
            
            if (level.getBlockState(newPos).blocksMotion()) {
                level.setBlock(newPos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
        }
    }

    private static void replicateDoorMechanism(ServerLevel level, TrapAnalyzer.TrapMechanism trap, BlockPos triggerPos) {
        level.setBlock(triggerPos, Blocks.IRON_DOOR.defaultBlockState(), 3);
        
        if (!trap.affectedBlocks.isEmpty()) {
            BlockPos other = trap.affectedBlocks.get(0);
            BlockPos newOther = other.offset(triggerPos.getX() - trap.triggerPos.getX(),
                                            triggerPos.getY() - trap.triggerPos.getY(),
                                            triggerPos.getZ() - trap.triggerPos.getZ());
            level.setBlock(newOther, Blocks.IRON_DOOR.defaultBlockState(), 3);
        }
    }

    private static void createDefaultShelter(ServerLevel level, BlockPos center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(center.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(center.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == -3 || x == 3 || z == -3 || z == 3) {
                    level.setBlock(center.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    level.setBlock(center.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                }
            }
        }

        level.setBlock(center.offset(0, 1, -3), Blocks.OAK_DOOR.defaultBlockState(), 3);
    }
}