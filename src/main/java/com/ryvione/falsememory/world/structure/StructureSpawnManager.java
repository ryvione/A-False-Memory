package com.ryvione.falsememory.world.structure;

import com.ryvione.falsememory.events.PhaseManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

public class StructureSpawnManager {

    private static final Random RNG = new Random();

    public static void checkAndSpawn(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        int phase = PhaseManager.getPhase(memory);

        if (phase >= 1 && !memory.wasTriggeredToday("struct_camp")) {
            if (RNG.nextInt(3) == 0) {
                BlockPos target = findSpawnPos(player, level, 80, 160);
                if (target != null) {
                    AbandonedCampStructure.spawn(level, target, memory);
                    memory.markTriggered("struct_camp");
                }
            }
        }

        if (phase >= 2 && !memory.wasTriggeredToday("struct_mine") && memory.inferredHomePos != null) {
            if (RNG.nextInt(4) == 0) {
                BlockPos target = findSpawnPos(player, level, 60, 120);
                if (target != null) {
                    MiningOutpostStructure.spawn(level, target, memory);
                    memory.markTriggered("struct_mine");
                }
            }
        }

        if (phase >= 3 && !memory.wasTriggeredToday("struct_safe")) {
            if (memory.loginPositions.size() >= 3) {
                BlockPos oldLogin = memory.loginPositions.get(0);
                SafehouseStructure.spawn(level, oldLogin, memory);
                memory.markTriggered("struct_safe");
            }
        }

        if (phase >= 4 && !memory.wasTriggeredToday("struct_archive") && memory.inferredHomePos != null) {
            BlockPos archivePos = memory.inferredHomePos.below(15);
            ArchiveStructure.spawn(level, archivePos, memory);
            memory.markTriggered("struct_archive");
             com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
            com.ryvione.falsememory.advancement.AdvancementTriggers.ARCHIVE_FOUND);
        }
    }

    private static BlockPos findSpawnPos(ServerPlayer player, ServerLevel level,
                                          int minDist, int maxDist) {
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = minDist + RNG.nextInt(maxDist - minDist);
            int x = (int)(player.getX() + Math.sin(angle) * dist);
            int z = (int)(player.getZ() + Math.cos(angle) * dist);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                x, z);
            if (y > 40) return new BlockPos(x, y, z);
        }
        return null;
    }
}