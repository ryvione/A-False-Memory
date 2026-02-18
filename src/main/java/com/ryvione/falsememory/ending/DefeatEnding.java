package com.ryvione.falsememory.ending;

import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class DefeatEnding {

    public static void trigger(ServerPlayer player, PlayerMemory memory) {
        TitleUtil.send(player,
            "§4It already knows where you'll spawn.",
            "",
            20, 100, 40);

        SoundUtil.playForPlayer(player, "minecraft:entity.enderman.stare",
            SoundSource.HOSTILE, 0.8f, 0.3f);

        if (memory != null) {
            memory.inManhunt = true;
        }
        com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
        com.ryvione.falsememory.advancement.AdvancementTriggers.DIED_TO_ONLY_ONE);
    }
}