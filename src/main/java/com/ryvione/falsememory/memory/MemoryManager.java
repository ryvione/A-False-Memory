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
    private static final String DATA_NAME = "falsememory_player_data";
    private static MemoryManager INSTANCE = null;

    private final Map<UUID, PlayerMemory> memories = new HashMap<>();

    public static MemoryManager getInstance() {
        return INSTANCE;
    }

    public static MemoryManager get(DimensionDataStorage storage) {
        INSTANCE = storage.computeIfAbsent(
            new SavedData.Factory<MemoryManager>(MemoryManager::new, MemoryManager::loadFromTag, null),
            DATA_NAME
        );
        return INSTANCE;
    }

    private static MemoryManager loadFromTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        MemoryManager mgr = new MemoryManager();
        CompoundTag playersTag = tag.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerMemory memory = PlayerMemory.load(playersTag.getCompound(key));
                mgr.memories.put(uuid, memory);
            } catch (Exception e) {
                LOGGER.warn("[FalseMemory] Failed to load memory for {}: {}", key, e.getMessage());
            }
        }
        return mgr;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        memories.forEach((uuid, memory) -> {
            try {
                playersTag.put(uuid.toString(), memory.save());
            } catch (Exception e) {
                LOGGER.warn("[FalseMemory] Failed to save memory for {}: {}", uuid, e.getMessage());
            }
        });
        tag.put("players", playersTag);
        return tag;
    }

    public PlayerMemory getOrCreate(ServerPlayer player) {
        return memories.computeIfAbsent(player.getUUID(), uuid -> {
            PlayerMemory memory = new PlayerMemory();
            memory.firstLoginPos = player.blockPosition();
            LOGGER.info("[FalseMemory] Created new memory for {}", player.getName().getString());
            return memory;
        });
    }

    public void markDirty(UUID uuid) {
        setDirty();
    }
}
