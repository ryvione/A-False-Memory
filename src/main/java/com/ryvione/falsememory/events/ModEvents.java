package com.ryvione.falsememory;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.events.HorrorEventScheduler;
import com.ryvione.falsememory.events.MultiplayerGuard;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
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

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        if (com.ryvione.falsememory.world.dimension.LostMemoriesDimension.isInDimension(player)) {
            com.ryvione.falsememory.world.dimension.LostMemoriesDimension.tickReplays(player);
            return;
        }

        MovementTracker.tick(player, memory);

        memory.heatmap.recordVisit(player.blockPosition());

        if (player.tickCount % 100 == 0) {
            HorrorEventScheduler.onPlayerTick(player, level);
        }

        if (player.tickCount % 200 == 0 && memory.inferredHomePos != null) {
            TrapAnalyzer.analyzeTrapAroundPos(level, player.blockPosition(), memory);
        }
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
        MultiplayerGuard.cleanup(player.getUUID());
        if (com.ryvione.falsememory.world.dimension.LostMemoriesDimension.isInDimension(player)) {
            com.ryvione.falsememory.world.dimension.LostMemoriesDimension.exitDimension(player);
        }
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

        if (event.getSource().getEntity() instanceof com.ryvione.falsememory.entity.TheOnlyOneEntity onlyOne) {
            com.ryvione.falsememory.ending.EndingManager.triggerEnding(
                player, com.ryvione.falsememory.ending.EndingType.DEFEAT);

            net.minecraft.world.item.ItemStack note = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.WRITTEN_BOOK);
            net.minecraft.nbt.CompoundTag noteTag = new net.minecraft.nbt.CompoundTag();
            noteTag.putString("title", "Recorded.");
            noteTag.putString("author", "???");
            net.minecraft.nbt.ListTag notePages = new net.minecraft.nbt.ListTag();
            float fleeHP = memory.averageHealthWhenFleeing / 2f;
            notePages.add(net.minecraft.nbt.StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                    net.minecraft.network.chat.Component.literal(
                        "You had " + String.format("%.1f", player.getHealth() / 2f) +
                        " hearts.\n\nYou had " + player.getInventory().countItem(
                            net.minecraft.world.item.Items.GOLDEN_APPLE) + " golden apples.\n\n" +
                        "You had everything.\n\nIt was not enough."
                    ), net.minecraft.core.RegistryAccess.EMPTY)));
            noteTag.put("pages", notePages);
            note.applyComponents(net.minecraft.core.component.DataComponentPatch.builder()
                .set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(noteTag)).build());

            if (!player.getInventory().add(note)) player.drop(note, false);
        }
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
        
        BuildingTracker.onBlockPlaced(player, memory, event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        
        memory.blockBreakCounts.merge(
            BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString(),
            1, Integer::sum);
        memory.recordBlockBrokenAt(event.getPos());
        BuildingTracker.onBlockBroken(player, memory, event.getPos(),
            event.getLevel().getBlockState(event.getPos()));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        CombatTracker.onPlayerHurt(player, memory, event.getSource(), event.getNewDamage());

        if (player.level() instanceof ServerLevel level) {
            level.getEntities(
                com.ryvione.falsememory.entity.ModEntities.THE_ONLY_ONE.get(),
                e -> e instanceof com.ryvione.falsememory.entity.TheOnlyOneEntity too
                    && player.getUUID().toString().equals(too.getTargetUUID())
            ).forEach(e -> {
                if (e instanceof com.ryvione.falsememory.entity.TheOnlyOneEntity too) {
                    too.notifyCombatHit(event.getSource(), event.getNewDamage());
                }
            });
        }
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
    public static void onInventoryOpened(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        InventoryTracker.onInventoryOpened(player, memory);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.loginPositions.add(player.blockPosition());
        if (player.getRespawnPosition() != null) {
            memory.preferredBedPos = player.getRespawnPosition();
        }
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerBedEnter(CanPlayerSleepEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        PlayerMemory memory = mgr.getOrCreate(player);
        memory.preferredBedPos = event.getPos();
        memory.sleepCount++;
        mgr.markDirty(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        HorrorEventScheduler.onWakeUp(player, level, memory);
        mgr.markDirty(player.getUUID());
    }
}
