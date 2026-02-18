package com.ryvione.falsememory.world.altar;

import com.ryvione.falsememory.Config;
import com.ryvione.falsememory.ending.EndingManager;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.entity.TheOnlyOneEntity;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

public class SummoningAltar {

    public static void attemptSummon(ServerPlayer player, ServerLevel level) {
        if (!Config.INSTANCE.enableOnlyOne.get()) return;

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        if (memory.knowledgeTier < 3) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§8It is not ready yet."));
            return;
        }

        boolean alreadyExists = !level.getEntities(
            ModEntities.THE_ONLY_ONE.get(),
            e -> e instanceof TheOnlyOneEntity too &&
                player.getUUID().toString().equals(too.getTargetUUID())
        ).isEmpty();

        if (alreadyExists) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§8It is already here."));
            return;
        }

        performSummonSequence(player, level, memory);
    }

    private static void performSummonSequence(ServerPlayer player, ServerLevel level,
                                               PlayerMemory memory) {
        SoundUtil.playForPlayer(player, "minecraft:entity.wither.spawn",
            SoundSource.HOSTILE, 1.0f, 0.3f);

        level.broadcastEntityEvent(player, (byte) 35);

        TitleUtil.send(player, "§0The world goes silent.", "", 20, 60, 20);

        player.getServer().execute(() -> {
            BlockPos spawnPos = player.blockPosition().offset(3, 0, 0);
            while (level.getBlockState(spawnPos).isAir() && spawnPos.getY() > 0)
                spawnPos = spawnPos.below();
            spawnPos = spawnPos.above();

            TheOnlyOneEntity onlyOne = ModEntities.THE_ONLY_ONE.get().create(level);
            if (onlyOne != null) {
                onlyOne.initFromPlayer(player);
                onlyOne.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(onlyOne);
                com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
                com.ryvione.falsememory.advancement.AdvancementTriggers.ONLY_ONE_SUMMONED);

                TitleUtil.send(player,
                    "§8It has your face.",
                    "§8It has your name.",
                    10, 80, 30);

                SoundUtil.playForPlayer(player, "minecraft:entity.enderman.stare",
                    SoundSource.HOSTILE, 0.8f, 0.2f);
            }
        });
    }
}