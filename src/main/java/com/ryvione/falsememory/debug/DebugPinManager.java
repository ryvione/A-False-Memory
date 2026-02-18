package com.ryvione.falsememory.debug;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DebugPinManager {

    private static final int CORRECT_PIN = 5545;
    private static final int MAX_ATTEMPTS = 10;

    private static final Map<UUID, Integer> failedAttempts = new HashMap<>();
    private static final Map<UUID, Boolean> unlockedSessions = new HashMap<>();
    private static final Map<UUID, Boolean> bannedFromDebug = new HashMap<>();

    public static boolean isUnlocked(ServerPlayer player) {
        return unlockedSessions.getOrDefault(player.getUUID(), false);
    }

    public static boolean isBanned(ServerPlayer player) {
        return bannedFromDebug.getOrDefault(player.getUUID(), false);
    }

    public static void tryPin(ServerPlayer player, int pin) {
        UUID uuid = player.getUUID();

        if (bannedFromDebug.getOrDefault(uuid, false)) {
            player.sendSystemMessage(Component.literal(
                "§4[DEBUG] You have been banned from debug access on this world."));
            return;
        }

        if (pin == CORRECT_PIN) {
            unlockedSessions.put(uuid, true);
            failedAttempts.put(uuid, 0);
            player.sendSystemMessage(Component.literal(
                "§a[DEBUG] Access granted. Welcome, developer."));
            return;
        }

        int attempts = failedAttempts.merge(uuid, 1, Integer::sum);
        int remaining = MAX_ATTEMPTS - attempts;

        if (attempts >= MAX_ATTEMPTS) {
            bannedFromDebug.put(uuid, true);
            unlockedSessions.put(uuid, false);
            player.sendSystemMessage(Component.literal(
                "§4[DEBUG] Too many incorrect attempts. You are banned from debug access on this world."));
            player.sendSystemMessage(Component.literal(
                "§4[DEBUG] Reason logged."));
            return;
        }

        player.sendSystemMessage(Component.literal(
            "§c[DEBUG] Incorrect PIN. " + remaining + " attempt(s) remaining."));
    }

    public static void lock(ServerPlayer player) {
        unlockedSessions.put(player.getUUID(), false);
        player.sendSystemMessage(Component.literal("§7[DEBUG] Session locked."));
    }
}