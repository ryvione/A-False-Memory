package com.ryvione.falsememory.world.dimension;

import com.ryvione.falsememory.FalseMemory;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LostMemoriesDimension {

    public static final ResourceKey<Level> LOST_MEMORIES_KEY =
        ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "lost_memories"));

    private static final Map<UUID, MemoryReplayManager> activeReplays = new HashMap<>();
    private static final Map<UUID, BlockPos> returnPositions = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> returnDimensions = new HashMap<>();

    public static boolean isDimensionLoaded(ServerPlayer player) {
        return player.getServer() != null &&
               player.getServer().getLevel(LOST_MEMORIES_KEY) != null;
    }

    public static void enterDimension(ServerPlayer player) {
        if (player.getServer() == null) return;
        ServerLevel lostLevel = player.getServer().getLevel(LOST_MEMORIES_KEY);
        if (lostLevel == null) {
            fallbackVoidExperience(player);
            return;
        }

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        returnPositions.put(player.getUUID(), player.blockPosition());
        returnDimensions.put(player.getUUID(), player.level().dimension());

        BlockPos spawnPos = new BlockPos(0, 64, 0);
        player.teleportTo(lostLevel,
            spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);

        MemoryReplayManager replay = new MemoryReplayManager(player, memory, lostLevel);
        activeReplays.put(player.getUUID(), replay);

        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(
            "\u00a78This is what it remembers.");
        player.sendSystemMessage(msg);
    }

    public static void exitDimension(ServerPlayer player) {
        if (player.getServer() == null) return;

        activeReplays.remove(player.getUUID());

        BlockPos returnPos = returnPositions.remove(player.getUUID());
        ResourceKey<Level> returnDim = returnDimensions.remove(player.getUUID());

        if (returnPos == null || returnDim == null) {
            ServerLevel overworld = player.getServer().overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
            return;
        }

        ServerLevel returnLevel = player.getServer().getLevel(returnDim);
        if (returnLevel == null) returnLevel = player.getServer().overworld();

        player.teleportTo(returnLevel,
            returnPos.getX() + 0.5, returnPos.getY(), returnPos.getZ() + 0.5, 0f, 0f);
    }

    public static void tickReplays(ServerPlayer player) {
        MemoryReplayManager replay = activeReplays.get(player.getUUID());
        if (replay == null) return;
        replay.tick();
        if (!replay.isReplaying()) {
            activeReplays.remove(player.getUUID());
        }
    }

    public static boolean isInDimension(ServerPlayer player) {
        return player.level().dimension().equals(LOST_MEMORIES_KEY);
    }

    public static boolean hasActiveReplay(ServerPlayer player) {
        return activeReplays.containsKey(player.getUUID());
    }

    private static void fallbackVoidExperience(ServerPlayer player) {
        com.ryvione.falsememory.util.TitleUtil.send(player,
            "\u00a78Lost Memories", "\u00a77It shows you everything.", 15, 80, 30);
        com.ryvione.falsememory.util.SoundUtil.playForPlayer(player,
            "minecraft:ambient.cave", net.minecraft.sounds.SoundSource.AMBIENT, 0.4f, 0.3f);

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        if (memory.inferredHomePos != null) {
            BlockPos home = memory.inferredHomePos;
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "\u00a78You always came back to " + home.getX() + ", " + home.getZ() + "."));
        }
        if (!memory.deathPositions.isEmpty()) {
            BlockPos death = memory.deathPositions.get(memory.deathPositions.size() - 1);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "\u00a78You died at " + death.getX() + " " + death.getY() + " " + death.getZ() + ". It was there."));
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78It remembers " + memory.totalDeaths + " deaths. " +
            memory.visitedBiomes.size() + " biomes. Every word you ever typed."));
    }
}
