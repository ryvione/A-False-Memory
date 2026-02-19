package com.ryvione.falsememory.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class TrapDeploymentSystem {

    private final Map<UUID, List<BlockPos>> deployedTraps = new HashMap<>();
    private final Map<UUID, Long> lastTrapActivation = new HashMap<>();

    public void deployTrapsAroundPosition(ServerPlayer player, BlockPos center, int radius, int trapCount) {
        UUID playerId = player.getUUID();
        if (!(player.level() instanceof ServerLevel level)) return;

        List<BlockPos> traps = deployedTraps.computeIfAbsent(playerId, k -> new ArrayList<>());
        traps.clear();

        Random rand = new Random();
        for (int i = 0; i < trapCount; i++) {
            int x = center.getX() + rand.nextInt(radius * 2) - radius;
            int z = center.getZ() + rand.nextInt(radius * 2) - radius;
            BlockPos trapPos = new BlockPos(x, center.getY(), z);

            createTrap(level, trapPos);
            traps.add(trapPos);
        }

        lastTrapActivation.put(playerId, System.currentTimeMillis());
    }

    public void activateTrapsIfPlayerAway(ServerPlayer player, BlockPos playerPos, BlockPos basePos, int minDistance) {
        if (playerPos.distSqr(basePos) < minDistance * minDistance) {
            return;
        }

        UUID playerId = player.getUUID();
        List<BlockPos> traps = deployedTraps.getOrDefault(playerId, new ArrayList<>());

        for (BlockPos trapPos : traps) {
            triggerTrap(player, trapPos);
        }
    }

    private void createTrap(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
    }

    private void triggerTrap(ServerPlayer player, BlockPos trapPos) {
        if (!(player.level() instanceof ServerLevel level)) return;

        if (level.getBlockState(trapPos).is(net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK)) {
            level.setBlock(trapPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public void clearTraps(ServerPlayer player) {
        deployedTraps.remove(player.getUUID());
        lastTrapActivation.remove(player.getUUID());
    }

    public int getTrapCount(ServerPlayer player) {
        return deployedTraps.getOrDefault(player.getUUID(), new ArrayList<>()).size();
    }
}
