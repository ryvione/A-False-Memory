package com.ryvione.falsememory.ending;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class DrawEnding {

    public static void trigger(ServerPlayer player, PlayerMemory memory) {
        player.sendSystemMessage(Component.literal("§8You both fell."));
        player.sendSystemMessage(Component.literal("§8Neither of you won."));
        player.sendSystemMessage(Component.literal("§8But it isn't gone."));
        player.sendSystemMessage(Component.literal("§8Something always comes back."));

        TitleUtil.send(player, "§8Neither Of You Were Real.", "", 20, 120, 40);

        SoundUtil.playForPlayer(player, "minecraft:entity.wither.death",
            SoundSource.HOSTILE, 0.5f, 0.5f);

        if (memory != null) {
            memory.knowledgeTier = 1;
            memory.inManhunt = false;
            memory.triggeredEvents.clear();
            com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
            com.ryvione.falsememory.advancement.AdvancementTriggers.DRAW);
        }
    }
}