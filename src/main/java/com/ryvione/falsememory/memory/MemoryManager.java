package com.ryvione.falsememory.memory;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryManager extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_KEY = "falsememory_player_memories";
    private final Map<UUID, PlayerMemory> memories = new HashMap<>();
    private static MemoryManager instance = null;

    public static MemoryManager get(DimensionDataStorage storage) {
        instance = storage.computeIfAbsent(
            new SavedData.Factory<>(MemoryManager::new, MemoryManager::load),
            DATA_KEY
        );
        return instance;
    }

    public static MemoryManager getInstance() {
        return instance;
    }

    public PlayerMemory getOrCreate(ServerPlayer player) {
        return memories.computeIfAbsent(player.getUUID(), uuid -> {
            LOGGER.info("[FalseMemory] Creating new memory for: {}", player.getName().getString());
            return new PlayerMemory();
        });
    }

    public PlayerMemory get(UUID uuid) {
        return memories.get(uuid);
    }

    public boolean hasMemory(UUID uuid) {
        return memories.containsKey(uuid);
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag allMemories = new CompoundTag();
        for (Map.Entry<UUID, PlayerMemory> entry : memories.entrySet()) {
            allMemories.put(entry.getKey().toString(), entry.getValue().save());
        }
        compound.put("memories", allMemories);
        return compound;
    }

    public static MemoryManager load(CompoundTag compound) {
        MemoryManager manager = new MemoryManager();
        CompoundTag allMemories = compound.getCompound("memories");
        for (String key : allMemories.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerMemory memory = PlayerMemory.load(allMemories.getCompound(key));
                manager.memories.put(uuid, memory);
            } catch (Exception e) {
                LOGGER.error("[FalseMemory] Failed to load memory for: {}", key, e);
            }
        }
        return manager;
    }

    public void markDirty(UUID uuid) {
        setDirty();
    }
}