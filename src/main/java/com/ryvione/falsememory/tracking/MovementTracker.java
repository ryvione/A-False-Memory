package com.ryvione.falsememory.tracking;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementTracker {

    private static final Map<UUID, BlockPos> lastPositions = new HashMap<>();
    private static final Map<UUID, Integer> stillTicks = new HashMap<>();

    public static void tick(ServerPlayer player, PlayerMemory memory) {
        UUID uuid = player.getUUID();
        BlockPos current = player.blockPosition();
        BlockPos last = lastPositions.get(uuid);

        if (last != null) {
            if (last.equals(current)) {
                stillTicks.merge(uuid, 1, Integer::sum);
            } else {
                stillTicks.put(uuid, 0);
            }
        }

        lastPositions.put(uuid, current);

        memory.recordFacing(player.getYRot());
        int chunkX = current.getX() >> 4;
        int chunkZ = current.getZ() >> 4;
        memory.recordChunkVisit(chunkX, chunkZ);
    }

    public static int getStillTicks(UUID uuid) {
        return stillTicks.getOrDefault(uuid, 0);
    }

    public static void clear(UUID uuid) {
        lastPositions.remove(uuid);
        stillTicks.remove(uuid);
    }
}