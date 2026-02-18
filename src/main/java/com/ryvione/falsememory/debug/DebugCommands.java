package com.ryvione.falsememory.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ryvione.falsememory.ending.EndingManager;
import com.ryvione.falsememory.ending.EndingType;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.entity.TheObsessedEntity;
import com.ryvione.falsememory.entity.TheOnlyOneEntity;
import com.ryvione.falsememory.entity.TheWitnessEntity;
import com.ryvione.falsememory.events.HorrorEvents;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.world.structure.*;
import com.ryvione.falsememory.world.altar.SummoningAltar;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.ryvione.falsememory.FalseMemory;

@EventBusSubscriber(modid = FalseMemory.MOD_ID)
public class DebugCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fm")
                .then(Commands.literal("pin")
                    .then(Commands.argument("code", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int pin = IntegerArgumentType.getInteger(ctx, "code");
                            DebugPinManager.tryPin(player, pin);
                            return 1;
                        })))
                .then(Commands.literal("lock")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DebugPinManager.lock(player);
                        return 1;
                    }))
                .then(Commands.literal("memory")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        dumpMemory(player);
                        return 1;
                    }))
                .then(Commands.literal("tier")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 3))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            int tier = IntegerArgumentType.getInteger(ctx, "value");
                            setTier(player, tier);
                            return 1;
                        })))
                .then(Commands.literal("day")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            int day = IntegerArgumentType.getInteger(ctx, "value");
                            setDay(player, day);
                            return 1;
                        })))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("entity", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String entity = StringArgumentType.getString(ctx, "entity");
                            spawnEntity(player, entity);
                            return 1;
                        })))
                .then(Commands.literal("event")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String name = StringArgumentType.getString(ctx, "event");
                            triggerEvent(player, name);
                            return 1;
                        })))
                .then(Commands.literal("structure")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String type = StringArgumentType.getString(ctx, "type");
                            spawnStructure(player, type);
                            return 1;
                        })))
                .then(Commands.literal("ending")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String type = StringArgumentType.getString(ctx, "type");
                            triggerEnding(player, type);
                            return 1;
                        })))
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        resetMemory(player);
                        return 1;
                    }))
                .then(Commands.literal("manhunt")
                    .then(Commands.argument("state", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String state = StringArgumentType.getString(ctx, "state");
                            setManhunt(player, state.equalsIgnoreCase("on"));
                            return 1;
                        })))
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        sendHelp(player);
                        return 1;
                    }))
        );
    }

    private static void dumpMemory(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) { player.sendSystemMessage(Component.literal("§c[DEBUG] No MemoryManager.")); return; }
        PlayerMemory m = mgr.getOrCreate(player);

        player.sendSystemMessage(Component.literal("§e[DEBUG] === MEMORY DUMP ==="));
        player.sendSystemMessage(Component.literal("§7Tier: §f" + m.knowledgeTier));
        player.sendSystemMessage(Component.literal("§7Day: §f" + m.worldDayCount));
        player.sendSystemMessage(Component.literal("§7Deaths: §f" + m.totalDeaths));
        player.sendSystemMessage(Component.literal("§7Sleep count: §f" + m.sleepCount));
        player.sendSystemMessage(Component.literal("§7Biomes visited: §f" + m.visitedBiomes.size()));
        player.sendSystemMessage(Component.literal("§7Home pos: §f" + (m.inferredHomePos != null ? m.inferredHomePos : "unknown")));
        player.sendSystemMessage(Component.literal("§7Weapon: §f" + m.preferredWeaponType));
        player.sendSystemMessage(Component.literal("§7Flee rate: §f" + String.format("%.0f%%", m.getCombatFleeRate() * 100)));
        player.sendSystemMessage(Component.literal("§7In manhunt: §f" + m.inManhunt));
        player.sendSystemMessage(Component.literal("§7Ticks played: §f" + m.totalTicksPlayed));
        player.sendSystemMessage(Component.literal("§7Events triggered: §f" + m.triggeredEvents.size()));
        player.sendSystemMessage(Component.literal("§7Chat history: §f" + m.chatHistory.size() + " entries"));
    }

    private static void setTier(ServerPlayer player, int tier) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        m.knowledgeTier = tier;
        mgr.markDirty(player.getUUID());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.ryvione.falsememory.network.SanityPacket(tier));
        player.sendSystemMessage(Component.literal("§a[DEBUG] Tier set to " + tier));
    }

    private static void setDay(ServerPlayer player, int day) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        m.worldDayCount = day;
        mgr.markDirty(player.getUUID());
        player.sendSystemMessage(Component.literal("§a[DEBUG] Day set to " + day));
    }

    private static void spawnEntity(ServerPlayer player, String entityName) {
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = player.blockPosition().offset(3, 0, 0);

        switch (entityName.toLowerCase()) {
            case "obsessed" -> {
                TheObsessedEntity e = ModEntities.THE_OBSESSED.get().create(level);
                if (e != null) {
                    e.setTargetPlayer(player.getUUID());
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned The Obsessed."));
                }
            }
            case "onlyone" -> {
                TheOnlyOneEntity e = ModEntities.THE_ONLY_ONE.get().create(level);
                if (e != null) {
                    e.initFromPlayer(player);
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned The Only One."));
                }
            }
            case "witness" -> {
                TheWitnessEntity e = ModEntities.THE_WITNESS.get().create(level);
                if (e != null) {
                    e.setTargetPlayer(player.getUUID());
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned The Witness."));
                }
            }
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown entity. Options: obsessed, onlyone, witness"));
        }
    }

    private static void triggerEvent(ServerPlayer player, String eventName) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);

        switch (eventName.toLowerCase()) {
            case "whisper" -> HorrorEvents.ambientWhisper(player, m);
            case "blockshift" -> HorrorEvents.subtleBlockShift(player, level, m);
            case "torch" -> HorrorEvents.missingTorch(player, level, m);
            case "footsteps" -> HorrorEvents.fakeFootsteps(player, m);
            case "book" -> HorrorEvents.memoryBookAppears(player, m);
            case "join" -> HorrorEvents.fakePlayerJoinMessage(player, m);
            case "death" -> HorrorEvents.yourOwnDeathSoundDistant(player, m);
            case "chat" -> HorrorEvents.fakeChatFromPast(player, m);
            case "replica" -> HorrorEvents.replicaBaseEvent(player, level, m);
            case "stalks" -> HorrorEvents.theObsessedStalks(player, level, m);
            case "echo" -> HorrorEvents.timelineEcho(player, level, m);
            case "predictive" -> HorrorEvents.predictivePlacement(player, level, m);
            case "invasion" -> HorrorEvents.baseInvasion(player, level, m);
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown event. Options: whisper, blockshift, torch, footsteps, book, join, death, chat, replica, stalks, echo, predictive, invasion"));
        }

        m.triggeredEvents.removeIf(e -> e.contains(eventName));
        player.sendSystemMessage(Component.literal("§a[DEBUG] Triggered event: " + eventName));
    }

    private static void spawnStructure(ServerPlayer player, String type) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        BlockPos pos = player.blockPosition().offset(5, 0, 5);

        switch (type.toLowerCase()) {
            case "camp" -> { AbandonedCampStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Abandoned Camp.")); }
            case "mine" -> { MiningOutpostStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Mining Outpost.")); }
            case "safe" -> { // SafehouseStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Safehouse.")); }
            case "archive" -> { ArchiveStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Archive.")); }
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown structure. Options: camp, mine, safe, archive"));
        }
    }

    private static void triggerEnding(ServerPlayer player, String type) {
        switch (type.toLowerCase()) {
            case "victory" -> EndingManager.triggerEnding(player, EndingType.VICTORY);
            case "defeat" -> EndingManager.triggerEnding(player, EndingType.DEFEAT);
            case "draw" -> EndingManager.triggerEnding(player, EndingType.DRAW);
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown ending. Options: victory, defeat, draw"));
        }
        player.sendSystemMessage(Component.literal("§a[DEBUG] Triggered ending: " + type));
    }

    private static void resetMemory(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        m.knowledgeTier = 0;
        m.triggeredEvents.clear();
        m.worldDayCount = 0;
        m.inManhunt = false;
        m.totalDeaths = 0;
        m.deathPositions.clear();
        mgr.markDirty(player.getUUID());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.ryvione.falsememory.network.SanityPacket(0));
        player.sendSystemMessage(Component.literal("§a[DEBUG] Memory reset."));
    }

    private static void setManhunt(ServerPlayer player, boolean on) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        m.inManhunt = on;
        mgr.markDirty(player.getUUID());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.ryvione.falsememory.network.SanityPacket(m.knowledgeTier));
        player.sendSystemMessage(Component.literal("§a[DEBUG] Manhunt: " + (on ? "ON" : "OFF")));
    }

    private static void sendHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§e[DEBUG] === FALSE MEMORY DEBUG ==="));
        player.sendSystemMessage(Component.literal("§7/fm pin <code>§f - Unlock debug (PIN required)"));
        player.sendSystemMessage(Component.literal("§7/fm lock§f - Lock your session"));
        player.sendSystemMessage(Component.literal("§7/fm memory§f - Dump all tracked data"));
        player.sendSystemMessage(Component.literal("§7/fm tier <0-3>§f - Set knowledge tier"));
        player.sendSystemMessage(Component.literal("§7/fm day <n>§f - Set tracked day count"));
        player.sendSystemMessage(Component.literal("§7/fm spawn <entity>§f - Spawn entity"));
        player.sendSystemMessage(Component.literal("§7/fm event <name>§f - Trigger horror event"));
        player.sendSystemMessage(Component.literal("§7/fm structure <type>§f - Spawn structure"));
        player.sendSystemMessage(Component.literal("§7/fm ending <type>§f - Trigger ending"));
        player.sendSystemMessage(Component.literal("§7/fm manhunt <on/off>§f - Toggle manhunt"));
        player.sendSystemMessage(Component.literal("§7/fm reset§f - Wipe all memory data"));
    }

    private static void deny(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
            "§c[DEBUG] Access denied. Use /fm pin <code> first."));
    }
}