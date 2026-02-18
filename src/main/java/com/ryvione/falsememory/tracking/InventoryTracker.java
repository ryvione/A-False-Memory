package com.ryvione.falsememory.tracking;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryTracker {

    private static final Map<UUID, Long> lastOpenTick = new HashMap<>();

    public static void onInventoryOpened(ServerPlayer player, PlayerMemory memory) {
        UUID uuid = player.getUUID();
        long tick = player.level().getGameTime();
        Long last = lastOpenTick.get(uuid);

        if (last != null) {
            long interval = tick - last;
            if (interval > 0 && interval < 12000) {
                memory.totalInventoryChecks++;
                long total = memory.totalInventoryChecks;
                memory.averageInventoryCheckInterval =
                    (memory.averageInventoryCheckInterval * (total - 1) + interval) / total;
            }
        }

        lastOpenTick.put(uuid, tick);
    }
}