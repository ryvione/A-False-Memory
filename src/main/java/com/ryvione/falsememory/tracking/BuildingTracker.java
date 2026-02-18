package com.ryvione.falsememory.tracking;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class BuildingTracker {

    public static void onBlockPlaced(ServerPlayer player, PlayerMemory memory,
                                      BlockPos pos, BlockState state) {
        memory.recordPlacedBlock(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        memory.blockPlacementCounts.merge(blockId, 1, Integer::sum);
    }

    public static void onBlockBroken(ServerPlayer player, PlayerMemory memory,
                                      BlockPos pos, BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        memory.blockBreakCounts.merge(blockId, 1, Integer::sum);
    }

    public static String inferBuildingStyle(PlayerMemory memory) {
        if (memory.blockPlacementCounts.isEmpty()) return "unknown";
        String mostPlaced = memory.getMostPlacedBlock();
        if (mostPlaced.contains("wood") || mostPlaced.contains("log") || mostPlaced.contains("plank")) return "wood";
        if (mostPlaced.contains("stone") || mostPlaced.contains("cobble")) return "stone";
        if (mostPlaced.contains("dirt") || mostPlaced.contains("sand")) return "dirt";
        return "mixed";
    }
}