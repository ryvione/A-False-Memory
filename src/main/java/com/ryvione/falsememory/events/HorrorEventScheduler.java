package com.ryvione.falsememory.events;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.events.MultiplayerGuard;
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
    private static final Random RNG    = new Random();

    public static void onDusk(ServerPlayer player, ServerLevel level) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.worldDayCount = level.getDayTime() / 24000L;
        memory.recalculateTier();

        long day  = memory.worldDayCount;
        int  tier = memory.knowledgeTier;
        int  grace = com.ryvione.falsememory.Config.INSTANCE.gracePeriodDays.get();

        if (!com.ryvione.falsememory.Config.INSTANCE.enabled.get()) return;

        if (day < grace) return;

        com.ryvione.falsememory.world.scar.WorldScarManager.updateScars(player, level, memory);
        com.ryvione.falsememory.world.structure.StructureSpawnManager.checkAndSpawn(player, level, memory);

        long effectiveDay = day;

        if (memory.falseVictoryDay >= 0) {
            long daysSinceVictory = day - memory.falseVictoryDay;
            if (daysSinceVictory < 3) return;
            if (daysSinceVictory == 3) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§8..."));
                com.ryvione.falsememory.util.SoundUtil.playForPlayer(player,
                    "minecraft:entity.enderman.ambient",
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.15f, 0.3f);
            }
            if (daysSinceVictory >= 5) {
                memory.falseVictoryDay = -1;
                memory.knowledgeTier = Math.max(1, tier);
                tier = memory.knowledgeTier;
                effectiveDay = day;
            } else {
                effectiveDay = grace + (daysSinceVictory - 3);
                tier = 0;
            }
        }

        List<Runnable> eligible = new ArrayList<>();

        int maxWhisper    = tier >= 2 ? 3 : 1;
        int maxFootsteps  = tier >= 2 ? 3 : 1;

        if (effectiveDay >= grace && com.ryvione.falsememory.Config.INSTANCE.enableBlockEvents.get()) {
            int wCount = memory.countTriggeredToday("whisper", maxWhisper);
            if (wCount < maxWhisper) eligible.add(() -> {
                int idx = memory.countTriggeredToday("whisper", maxWhisper);
                com.ryvione.falsememory.util.SoundUtil.playForPlayer(player,
                    "minecraft:entity.player.hurt",
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.15f, 0.2f + RNG.nextFloat() * 0.15f);
                memory.markTriggeredIndexed("whisper", idx);
            });
            int fCount = memory.countTriggeredToday("fake_footsteps", maxFootsteps);
            if (fCount < maxFootsteps) eligible.add(() -> {
                int idx = memory.countTriggeredToday("fake_footsteps", maxFootsteps);
                HorrorEvents.fakeFootstepsMulti(player, memory, idx);
                memory.markTriggeredIndexed("fake_footsteps", idx);
            });
            eligible.add(() -> HorrorEvents.missingTorch(player, level, memory));
            eligible.add(() -> HorrorEvents.subtleBlockShift(player, level, memory));
            
            eligible.add(() -> HorrorEvents.rooftopFootsteps(player, level, memory));
            
            eligible.add(() -> HorrorEvents.miningPresence(player, level, memory));
        }

        if (effectiveDay >= grace + 3 && tier >= 1) {
            eligible.add(() -> HorrorEvents.chestSlightlyOpen(player, level, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableChatEvents.get()) {
                eligible.add(() -> HorrorEvents.signTextChanged(player, level, memory));
            }
            eligible.add(() -> HorrorEvents.yourOwnDeathSoundDistant(player, memory));
            eligible.add(() -> HorrorEvents.sleepHorror(player, level, memory));
            
            eligible.add(() -> HorrorEvents.sessionTimeComment(player, memory));
            
            eligible.add(() -> HorrorEvents.falseSysMessage(player, memory));
            
            eligible.add(() -> HorrorEvents.escalatingNotes(player, level, memory));
        }

        if (effectiveDay >= grace + 7 && tier >= 2) {
            eligible.add(() -> HorrorEvents.fakePlayerJoinMessage(player, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableMemoryBook.get()) {
                eligible.add(() -> HorrorEvents.memoryBookAppears(player, memory));
            }
            eligible.add(() -> HorrorEvents.blockFromYourPastAppears(player, level, memory));
            eligible.add(() -> HorrorEvents.baseInvasion(player, level, memory));
            eligible.add(() -> HorrorEvents.timelineEcho(player, level, memory));
            eligible.add(() -> HorrorEvents.intelligenceReveal(player, memory));
            
            eligible.add(() -> HorrorEvents.inventoryObservation(player, memory));
            
            eligible.add(() -> HorrorEvents.currentInventorySpook(player, memory));
            
            eligible.add(() -> HorrorEvents.fourthWallBreak(player, memory));
            
            eligible.add(() -> HorrorEvents.silentWatcher(player, level, memory));
            
            eligible.add(() -> HorrorEvents.predictiveChest(player, level, memory));
        }

        if (effectiveDay >= grace + 11 && tier >= 3) {
            eligible.add(() -> HorrorEvents.theObsessedStalks(player, level, memory));
            eligible.add(() -> HorrorEvents.replicaBaseEventCorrupted(player, level, memory));
            if (com.ryvione.falsememory.Config.INSTANCE.enableChatEvents.get()) {
                eligible.add(() -> HorrorEvents.fakeChatMutated(player, memory));
                
                eligible.add(() -> HorrorEvents.echoedChatCorruption(player, memory));
            }
            eligible.add(() -> HorrorEvents.standingWhereYouStand(player, level, memory));
            eligible.add(() -> HorrorEvents.predictivePlacement(player, level, memory));
            
            eligible.add(() -> HorrorEvents.anticipatoryBlockPlacement(player, level, memory));
            
            eligible.add(() -> HorrorEvents.deathPositionClone(player, level, memory));
        }

        if (!eligible.isEmpty()) {
            if (MultiplayerGuard.isThrottled(player)) return;
            int count = tier >= 2 ? (RNG.nextInt(3) + 2) : 1;
            if (MultiplayerGuard.isMultiplayer(player)) count = 1;
            count = Math.min(count, eligible.size());
            Collections.shuffle(eligible, RNG);
            for (int i = 0; i < count; i++) {
                try { eligible.get(i).run(); }
                catch (Exception e) {
                    LOGGER.error("[FalseMemory] Event failed for {}: {}",
                        player.getName().getString(), e.getMessage());
                }
            }
            MultiplayerGuard.markFired(player);
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
        if (memory.firstLoginPos == null) memory.firstLoginPos = player.blockPosition();

        if (day >= 7 && !memory.wasTriggeredToday("login_event")) {
            HorrorEvents.loginAmbience(player, memory);
            memory.markTriggered("login_event");
        }

        if (memory.inManhunt) {
            HorrorEvents.manhuntLoginAmbience(player, memory);
            ManhuntManager.onLoginDuringManhunt(player, level, memory);
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
        memory.heatmap.recordVisit(player.blockPosition());

        memory.worldDayCount = level.getDayTime() / 24000L;

        memory.lastHotbarSnapshot.clear();
        for (int i = 0; i < 9; i++) {
            var item = player.getInventory().getItem(i);
            if (!item.isEmpty()) {
                memory.lastHotbarSnapshot.add(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(item.getItem()).toString());
            }
        }

        var biomeHolder = level.getBiome(player.blockPosition());
        biomeHolder.unwrapKey().ifPresent(key -> {
            String bio = key.location().toString();
            memory.visitedBiomes.add(bio);
            if (memory.mostFrequentBiome == null) memory.mostFrequentBiome = bio;
        });

        if (memory.totalTicksPlayed % 6000 == 0) {
            int oldTier = memory.knowledgeTier;
            memory.recalculateTier();
            mgr.markDirty(player.getUUID());
            if (memory.knowledgeTier != oldTier) {
                PacketDistributor.sendToPlayer(player, new SanityPacket(memory.knowledgeTier));
                if (memory.knowledgeTier >= 3) {
                    com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
                        com.ryvione.falsememory.advancement.AdvancementTriggers.TIER_3);
                }
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

        if (memory.totalTicksPlayed % 601 == 0) {
            boolean hasWatcher = memory.triggeredEvents.stream()
                .anyMatch(e -> e.startsWith("watcher_entity_"));
            if (hasWatcher) {
                HorrorEvents.silentWatcherDespawn(player, level, memory);
            }
        }
    }

    public static void onWakeUp(ServerPlayer player, ServerLevel level, com.ryvione.falsememory.memory.PlayerMemory memory) {
        if (memory.knowledgeTier < 1) return;
        
        memory.triggeredEvents.removeIf(e -> e.equals("sleep_horror"));
        HorrorEvents.sleepHorror(player, level, memory);
        
        HorrorEvents.wakeUpWrongRoom(player, level, memory);
    }
}
