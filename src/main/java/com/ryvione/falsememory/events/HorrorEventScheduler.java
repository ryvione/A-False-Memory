package com.ryvione.falsememory.events;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.network.SanityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HorrorEventScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG = new Random();

    public static void onDusk(ServerPlayer player, ServerLevel level) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.worldDayCount = level.getDayTime() / 24000L;
        memory.recalculateTier();
        
        long day = memory.worldDayCount;
        int tier = memory.knowledgeTier;

        int gracePeriod = com.ryvione.falsememory.Config.INSTANCE.gracePeriodDays.get();
        if (day < gracePeriod) return;
        if (!com.ryvione.falsememory.Config.INSTANCE.enabled.get()) return;

        com.ryvione.falsememory.world.scar.WorldScarManager.updateScars(player, level, memory);
        com.ryvione.falsememory.world.structure.StructureSpawnManager.checkAndSpawn(player, level, memory);

        List<Runnable> eligible = new ArrayList<>();

        if (day >= gracePeriod && com.ryvione.falsememory.Config.INSTANCE.enableBlockEvents.get()) {
            eligible.add(() -> HorrorEvents.ambientWhisper(player, memory));
            eligible.add(() -> HorrorEvents.missingTorch(player, level, memory));
            eligible.add(() -> HorrorEvents.subtleBlockShift(player, level, memory));
            eligible.add(() -> HorrorEvents.fakeFootsteps(player, memory));
        }

        if (day >= gracePeriod + 3 && tier >= 1) {
            eligible.add(() -> HorrorEvents.chestSlightlyOpen(player, level, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableChatEvents.get()) {
                eligible.add(() -> HorrorEvents.signTextChanged(player, level, memory));
            }
        }

        if (day >= gracePeriod + 7 && tier >= 2) {
            eligible.add(() -> HorrorEvents.fakePlayerJoinMessage(player, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableMemoryBook.get()) {
                eligible.add(() -> HorrorEvents.memoryBookAppears(player, memory));
            }
            eligible.add(() -> HorrorEvents.yourOwnDeathSoundDistant(player, memory));
            eligible.add(() -> HorrorEvents.blockFromYourPastAppears(player, level, memory));
            eligible.add(() -> HorrorEvents.baseInvasion(player, level, memory));
            eligible.add(() -> HorrorEvents.timelineEcho(player, level, memory));
        }

        if (day >= gracePeriod + 11 && tier >= 3) {
            eligible.add(() -> HorrorEvents.theObsessedStalks(player, level, memory));
            eligible.add(() -> HorrorEvents.replicaBaseEvent(player, level, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableChatEvents.get()) {
                eligible.add(() -> HorrorEvents.fakeChatFromPast(player, memory));
            }
            eligible.add(() -> HorrorEvents.standingWhereYouStand(player, level, memory));
            eligible.add(() -> HorrorEvents.predictivePlacement(player, level, memory));
        }

        if (!eligible.isEmpty()) {
            int count = tier >= 2 ? (RNG.nextInt(3) + 1) : 1;
            count = Math.min(count, eligible.size());
            Collections.shuffle(eligible, RNG);

            for (int i = 0; i < count; i++) {
                try {
                    eligible.get(i).run();
                } catch (Exception e) {
                    LOGGER.error("[FalseMemory] Event failed for {}: {}", player.getName().getString(), e.getMessage());
                }
            }
            mgr.markDirty(player.getUUID());
        }
    }

    public static void onLogin(ServerPlayer player, ServerLevel level) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        long day = level.getDayTime() / 24000L;
        memory.worldDayCount = day;

        memory.loginPositions.add(player.blockPosition());
        if (memory.firstLoginPos == null) {
            memory.firstLoginPos = player.blockPosition();
        }

        if (day >= 7 && !memory.wasTriggeredToday("login_event")) {
            HorrorEvents.loginAmbience(player, memory);
            memory.markTriggered("login_event");
        }

        if (memory.inManhunt) {
            HorrorEvents.manhuntLoginAmbience(player, memory);
            com.ryvione.falsememory.events.ManhuntManager.onLoginDuringManhunt(player, level, memory);
        }

        PacketDistributor.sendToPlayer(player, new SanityPacket(memory.knowledgeTier));
        mgr.markDirty(player.getUUID());
    }

    public static void onPlayerTick(ServerPlayer player, ServerLevel level) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);

        memory.totalTicksPlayed++;
        memory.recordFacing(player.getYRot());

        int chunkX = player.blockPosition().getX() >> 4;
        int chunkZ = player.blockPosition().getZ() >> 4;
        memory.recordChunkVisit(chunkX, chunkZ);

        memory.worldDayCount = level.getDayTime() / 24000L;

        memory.lastHotbarSnapshot.clear();
        for (int i = 0; i < 9; i++) {
            var item = player.getInventory().getItem(i);
            if (!item.isEmpty()) {
                memory.lastHotbarSnapshot.add(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString()
                );
            }
        }

        var biomeHolder = level.getBiome(player.blockPosition());
        biomeHolder.unwrapKey().ifPresent(key -> {
            String biomeName = key.location().toString();
            memory.visitedBiomes.add(biomeName);
            if (memory.mostFrequentBiome == null) memory.mostFrequentBiome = biomeName;
        });

        if (memory.totalTicksPlayed % 6000 == 0) {
            int oldTier = memory.knowledgeTier;
            memory.recalculateTier();
            mgr.markDirty(player.getUUID());

            if (memory.knowledgeTier != oldTier) {
                PacketDistributor.sendToPlayer(player,
                    new SanityPacket(memory.knowledgeTier));
            }
        }

        long timeOfDay = level.getDayTime() % 24000;
        if (timeOfDay >= 12500 && timeOfDay < 12600) {
            String duskKey = "dusk_day" + memory.worldDayCount;
            if (!memory.triggeredEvents.contains(duskKey)) {
                memory.triggeredEvents.add(duskKey);
                onDusk(player, level);
            }
        }
    }
}