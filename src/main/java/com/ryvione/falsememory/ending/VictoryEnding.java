package com.ryvione.falsememory.ending;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Random;

public class VictoryEnding {

    private static final Random RNG = new Random();

    public static void trigger(ServerPlayer player, PlayerMemory memory) {
        String finalWords = "...";
        if (memory != null && !memory.chatHistory.isEmpty()) {
            finalWords = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
        }

        com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
            com.ryvione.falsememory.advancement.AdvancementTriggers.ONLY_ONE_DEFEATED);

        player.sendSystemMessage(Component.literal("§8\"" + finalWords + "\""));
        player.sendSystemMessage(Component.literal("§8silence"));

        TitleUtil.send(player, "§fIt's over.", "", 20, 100, 40);

        SoundUtil.playForPlayer(player, "minecraft:entity.wither.death",
            SoundSource.HOSTILE, 0.3f, 0.6f);

        if (memory != null) {
            memory.falseVictoryDay = memory.worldDayCount;
            memory.inManhunt = false;
            memory.knowledgeTier = 0;

            memory.triggeredEvents.removeIf(e -> !e.startsWith("victory_reset"));
        }
    }
}
