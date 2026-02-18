package com.ryvione.falsememory;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.events.HorrorEventScheduler;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.CombatTracker;
import com.ryvione.falsememory.tracking.InventoryTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingBedSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = FalseMemory.MOD_ID)
public class ModEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % 100 != 0) return;
        HorrorEventScheduler.onPlayerTick(player, level);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        var storage = level.getServer().overworld().getDataStorage();
        MemoryManager.get(storage);
        HorrorEventScheduler.onLogin(player, level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr != null) mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.deathPositions.add(player.blockPosition());
        memory.totalDeaths++;
        memory.lastDeathCause = event.getSource().getMsgId();
        memory.deathCauseHistory.merge(memory.lastDeathCause, 1, Integer::sum);
        memory.recalculateTier();
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        String itemId = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).toString();
        memory.craftedItems.merge(itemId, 1, Integer::sum);
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.recordPlacedBlock(event.getPos());
        String blockId = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString();
        memory.blockPlacementCounts.merge(blockId, 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.recordChat(event.getMessage().getString());
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerSleep(LivingBedSleepingEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.sleepCount++;
        memory.preferredBedPos = event.getPos();
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);
        com.ryvione.falsememory.tracking.BuildingTracker.onBlockBroken(
            player, memory, event.getPos(),
            event.getLevel().getBlockState(event.getPos()));
    }

    @SubscribeEvent
    public static void onBlockPlacedFull(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);
        com.ryvione.falsememory.tracking.BuildingTracker.onBlockPlaced(
            player, memory, event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        CombatTracker.onPlayerHurt(player, memory, event.getSource(), event.getAmount());
    }
}