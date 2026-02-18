package com.ryvione.falsememory.events;

import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.entity.TheOnlyOneEntity;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.network.SanityPacket;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class ManhuntManager {

    private static final Random RNG = new Random();

    public static void activate(ServerPlayer player, ServerLevel level,
                                 TheOnlyOneEntity onlyOne, PlayerMemory memory) {
        memory.inManhunt = true;
        memory.manhuntStartDay = memory.worldDayCount;
        PacketDistributor.sendToPlayer(player, new SanityPacket(memory.knowledgeTier));
        TitleUtil.send(player, "§4It's not over.", "§8Run.", 10, 60, 20);
        SoundUtil.playForPlayer(player, "minecraft:entity.wither.spawn", SoundSource.HOSTILE, 0.6f, 0.4f);
    }

    public static void onLoginDuringManhunt(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        boolean onlyOneExists = !level.getEntities(
            ModEntities.THE_ONLY_ONE.get(),
            e -> e instanceof TheOnlyOneEntity too &&
                player.getUUID().toString().equals(too.getTargetUUID())
        ).isEmpty();

        if (!onlyOneExists) {
            spawnOnlyOneNearPlayer(player, level, memory);
        }

        TitleUtil.send(player, "§4It knows where you spawn.", "§8It's already here.", 10, 60, 20);
        SoundUtil.playForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.5f, 0.3f);
    }

    private static void spawnOnlyOneNearPlayer(ServerPlayer player, ServerLevel level,
                                                PlayerMemory memory) {
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 20 + RNG.nextInt(10);
        double sx = player.getX() + Math.sin(angle) * dist;
        double sz = player.getZ() + Math.cos(angle) * dist;

        BlockPos spawnPos = BlockPos.containing(sx, player.getY(), sz);
        while (level.getBlockState(spawnPos).isAir() && spawnPos.getY() > 0) spawnPos = spawnPos.below();
        spawnPos = spawnPos.above();

        TheOnlyOneEntity onlyOne = ModEntities.THE_ONLY_ONE.get().create(level);
        if (onlyOne != null) {
            onlyOne.initFromPlayer(player);
            onlyOne.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            level.addFreshEntity(onlyOne);
        }
    }

    public static void deactivate(ServerPlayer player, PlayerMemory memory) {
        memory.inManhunt = false;
        PacketDistributor.sendToPlayer(player, new SanityPacket(memory.knowledgeTier));
    }
}