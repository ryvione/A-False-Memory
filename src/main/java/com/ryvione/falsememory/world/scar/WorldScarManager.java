package com.ryvione.falsememory.world.scar;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class WorldScarManager {

    private static final Map<UUID, Set<Long>> placedScars = new HashMap<>();

    public static void updateScars(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.knowledgeTier < 2) return;

        UUID uuid = player.getUUID();
        Set<Long> placed = placedScars.computeIfAbsent(uuid, k -> new HashSet<>());

        for (BlockPos deathPos : memory.deathPositions) {
            long key = deathPos.asLong();
            if (!placed.contains(key)) {
                ScarPlacer.placeScar(level, deathPos);
                placed.add(key);
            }
        }
    }
}