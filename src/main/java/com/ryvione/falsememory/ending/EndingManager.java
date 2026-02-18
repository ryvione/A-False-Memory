package com.ryvione.falsememory.ending;

import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.network.EndingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class EndingManager {

    public static void triggerEnding(ServerPlayer player, EndingType type) {
        MemoryManager mgr = MemoryManager.getInstance();
        PlayerMemory memory = mgr != null ? mgr.getOrCreate(player) : null;

        switch (type) {
            case VICTORY -> VictoryEnding.trigger(player, memory);
            case DEFEAT -> DefeatEnding.trigger(player, memory);
            case DRAW -> DrawEnding.trigger(player, memory);
        }

        PacketDistributor.sendToPlayer(player, new EndingPacket(type.ordinal()));
    }
}