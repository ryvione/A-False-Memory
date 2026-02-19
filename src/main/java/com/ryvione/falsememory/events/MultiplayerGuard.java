package com.ryvione.falsememory.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiplayerGuard {

    private static final Map<UUID, Long> lastEventTick = new HashMap<>();
    private static final int MIN_TICKS_BETWEEN_SERVER_EVENTS = 200;

    public static boolean canFireServerWideEvent(MinecraftServer server) {
        int onlinePlayers = server.getPlayerList().getPlayerCount();
        return onlinePlayers <= 1;
    }

    public static boolean isThrottled(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = lastEventTick.get(player.getUUID());
        if (last == null) return false;
        return (now - last) < MIN_TICKS_BETWEEN_SERVER_EVENTS;
    }

    public static void markFired(ServerPlayer player) {
        lastEventTick.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean isMultiplayer(ServerPlayer player) {
        if (player.getServer() == null) return false;
        return player.getServer().getPlayerList().getPlayerCount() > 1;
    }

    public static void cleanup(UUID uuid) {
        lastEventTick.remove(uuid);
    }
}
