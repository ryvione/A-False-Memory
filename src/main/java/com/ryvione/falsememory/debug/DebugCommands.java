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
import com.ryvione.falsememory.events.ManhuntManager;
import com.ryvione.falsememory.events.HorrorEventScheduler;
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

import java.util.ArrayList;
import java.util.List;

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
                .then(Commands.literal("debug")
                    .then(Commands.argument("action", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String action = StringArgumentType.getString(ctx, "action");
                            handleDebugAction(player, action);
                            return 1;
                        })))
                .then(Commands.literal("data")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        listData(player);
                        return 1;
                    }))
                .then(Commands.literal("seed")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        seedMemory(player);
                        return 1;
                    }))
                .then(Commands.literal("test")
                    .then(Commands.argument("phase", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String phase = StringArgumentType.getString(ctx, "phase");
                            runTest(player, phase);
                            return 1;
                        })))
                .then(Commands.literal("falsevictory")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        testFalseVictory(player);
                        return 1;
                    }))
                .then(Commands.literal("checklist")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        printChecklist(player);
                        return 1;
                    }))
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                        sendHelp(player);
                        return 1;
                    }))
        );

        dispatcher.register(
            Commands.literal("falsememory")
                .then(Commands.literal("debug")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        sendHelp(player);
                        return 1;
                    })
                    .then(Commands.argument("action", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!DebugPinManager.isUnlocked(player)) { deny(player); return 0; }
                            String action = StringArgumentType.getString(ctx, "action");
                            
                            handleDebugAction(player, action);
                            return 1;
                        })))
        );
    }

    private static void seedMemory(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);

        m.chatHistory.clear();
        m.chatHistory.add("I need to find diamonds");
        m.chatHistory.add("why does it keep following me");
        m.chatHistory.add("ok this is actually terrifying");
        m.chatHistory.add("hello?");
        m.chatHistory.add("im going to kill it");

        m.deathPositions.add(player.blockPosition().offset(10, 0, 5));
        m.deathPositions.add(player.blockPosition().offset(-3, 0, 12));
        m.totalDeaths = 2;
        m.lastDeathCause = "mob";
        m.deathCauseHistory.put("mob", 2);

        m.inferredHomePos = player.blockPosition();
        m.preferredBedPos = player.blockPosition().offset(2, 0, 0);

        m.preferredWeaponType = "sword";
        m.averageHealthWhenFleeing = 7.0f;
        m.combatsFled = 3;
        m.combatsStoodGround = 1;
        m.totalCombatEvents = 4;
        m.usesPotions = false;
        m.usesRangedWeapons = false;

        m.mostFrequentBiome = "minecraft:forest";
        m.visitedBiomes.add("minecraft:forest");
        m.visitedBiomes.add("minecraft:plains");
        m.visitedBiomes.add("minecraft:mountains");

        m.loginPositions.add(player.blockPosition().offset(30, 0, 0));
        m.loginPositions.add(player.blockPosition().offset(15, 0, -10));
        m.loginPositions.add(player.blockPosition());

        for (int i = 0; i < 15; i++) {
            m.recentlyPlacedBlocks.add(player.blockPosition().offset(i - 7, 0, i % 3));
        }

        m.craftedItems.put("minecraft:iron_sword", 2);
        m.craftedItems.put("minecraft:chest", 5);
        m.craftedItems.put("minecraft:crafting_table", 1);

        m.worldDayCount = 10;
        m.knowledgeTier = 2;
        m.totalTicksPlayed = 20 * 60 * 90;

        m.recalculateTier();
        mgr.markDirty(player.getUUID());

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.ryvione.falsememory.network.SanityPacket(m.knowledgeTier));

        player.sendSystemMessage(Component.literal("§a[DEBUG] Memory seeded with fake player data."));
        player.sendSystemMessage(Component.literal("§7 - 5 chat messages, 2 deaths, home set, bed set"));
        player.sendSystemMessage(Component.literal("§7 - combat profile, 3 biomes, login history, 15 placed blocks"));
        player.sendSystemMessage(Component.literal("§7 - tier: " + m.knowledgeTier + ", day: 10"));
        player.sendSystemMessage(Component.literal("§eRun §f/fm test all§e to begin testing."));
    }

    private static void runTest(ServerPlayer player, String phase) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);

        m.triggeredEvents.clear();

        switch (phase.toLowerCase()) {
            case "all" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Running full mod test — all phases."));
                player.sendSystemMessage(Component.literal("§7Watch for effects. Each fires with 1 second gap (async)."));
                runPhaseTest(player, level, m, 1);
                runPhaseTest(player, level, m, 2);
                runPhaseTest(player, level, m, 3);
                runPhaseTest(player, level, m, 4);
                player.sendSystemMessage(Component.literal("§a[TEST] All events triggered. Check §f/fm checklist§a for what to verify."));
            }
            case "1", "phase1" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Phase 1 — Subtle wrongness"));
                runPhaseTest(player, level, m, 1);
            }
            case "2", "phase2" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Phase 2 — It knows something"));
                runPhaseTest(player, level, m, 2);
            }
            case "3", "phase3" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Phase 3 — It knows YOU"));
                runPhaseTest(player, level, m, 3);
            }
            case "4", "phase4" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Phase 4 — Confrontation"));
                runPhaseTest(player, level, m, 4);
            }
            case "combat" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Combat — Spawning TheOnlyOne 5 blocks away"));
                BlockPos pos = player.blockPosition().offset(5, 0, 0);
                TheOnlyOneEntity e = ModEntities.THE_ONLY_ONE.get().create(level);
                if (e != null) {
                    e.initFromPlayer(player);
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("§7Fight it. On death it should drop a book and trigger DEFEAT ending."));
                    player.sendSystemMessage(Component.literal("§7Kill it to verify VICTORY ending + false victory restart."));
                }
            }
            case "sleep" -> {
                player.sendSystemMessage(Component.literal("§e[TEST] Sleep horror — go sleep in a bed now."));
                player.sendSystemMessage(Component.literal("§7Verify: screen inverts on entering bed, static+shake on wake."));
                player.sendSystemMessage(Component.literal("§7Server side: block near bed should move while you sleep."));
            }
            default -> player.sendSystemMessage(Component.literal(
                "§c[TEST] Unknown phase. Options: all, 1, 2, 3, 4, combat, sleep"));
        }
    }

    private static void runPhaseTest(ServerPlayer player, ServerLevel level, PlayerMemory m, int phase) {
        m.triggeredEvents.clear();
        switch (phase) {
            case 1 -> {
                player.sendSystemMessage(Component.literal("§8── Phase 1 ──────────────────────────"));
                HorrorEvents.ambientWhisper(player, m); m.triggeredEvents.removeIf(e -> e.contains("whisper"));
                HorrorEvents.fakeFootsteps(player, m); m.triggeredEvents.removeIf(e -> e.contains("footsteps"));
                HorrorEvents.missingTorch(player, level, m); m.triggeredEvents.removeIf(e -> e.contains("torch"));
                HorrorEvents.subtleBlockShift(player, level, m);
                player.sendSystemMessage(Component.literal("§7 whisper, footsteps, missing_torch, block_shift fired"));
            }
            case 2 -> {
                player.sendSystemMessage(Component.literal("§8── Phase 2 ──────────────────────────"));
                HorrorEvents.chestSlightlyOpen(player, level, m); m.triggeredEvents.removeIf(e -> e.contains("chest"));
                HorrorEvents.signTextChanged(player, level, m); m.triggeredEvents.removeIf(e -> e.contains("sign"));
                HorrorEvents.yourOwnDeathSoundDistant(player, m); m.triggeredEvents.removeIf(e -> e.contains("death_sound"));
                HorrorEvents.sleepHorror(player, level, m);
                player.sendSystemMessage(Component.literal("§7 chest_open, sign_text, death_sound, sleep_horror fired"));
            }
            case 3 -> {
                player.sendSystemMessage(Component.literal("§8── Phase 3 ──────────────────────────"));
                HorrorEvents.fakePlayerJoinMessage(player, m); m.triggeredEvents.removeIf(e -> e.contains("join"));
                HorrorEvents.memoryBookAppears(player, m); m.triggeredEvents.removeIf(e -> e.contains("book"));
                HorrorEvents.blockFromYourPastAppears(player, level, m);
                HorrorEvents.intelligenceReveal(player, m); m.triggeredEvents.removeIf(e -> e.contains("intel"));
                HorrorEvents.timelineEcho(player, level, m);
                player.sendSystemMessage(Component.literal("§7 fake_join, memory_book, past_block, intel_reveal, timeline_echo fired"));
            }
            case 4 -> {
                player.sendSystemMessage(Component.literal("§8── Phase 4 ──────────────────────────"));
                HorrorEvents.theObsessedStalks(player, level, m); m.triggeredEvents.removeIf(e -> e.contains("obsessed"));
                HorrorEvents.replicaBaseEventCorrupted(player, level, m); m.triggeredEvents.removeIf(e -> e.contains("replica"));
                HorrorEvents.fakeChatMutated(player, m); m.triggeredEvents.removeIf(e -> e.contains("fake_chat"));
                HorrorEvents.standingWhereYouStand(player, level, m);
                HorrorEvents.predictivePlacement(player, level, m);
                player.sendSystemMessage(Component.literal("§7 obsessed_stalks, replica_corrupted, mutated_chat, standing, predictive fired"));
            }
        }
    }

    private static void testFalseVictory(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);

        EndingManager.triggerEnding(player, EndingType.VICTORY);
        mgr.markDirty(player.getUUID());

        player.sendSystemMessage(Component.literal("§e[TEST] False victory triggered."));
        player.sendSystemMessage(Component.literal("§7falseVictoryDay set to: " + m.falseVictoryDay));
        player.sendSystemMessage(Component.literal("§7Run §f/fm day " + (m.falseVictoryDay + 3) + "§7 then §f/fm event whisper§7 — should stay silent for 2 days."));
        player.sendSystemMessage(Component.literal("§7Run §f/fm day " + (m.falseVictoryDay + 5) + "§7 — events should resume."));
    }

    private static void printChecklist(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§e[CHECKLIST] False Memory — Dev Test Checklist"));
        player.sendSystemMessage(Component.literal("§8Run §f/fm seed§8 then §f/fm test all§8 before starting."));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Phase 1 — Subtle Wrongness:"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fWhisper — hear distant player hurt sound (quiet, low pitch)"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fFootsteps — hear stone step sounds nearby"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fMissing torch — torch disappears near home"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fBlock shift — one block near home moved 1 block"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Phase 2 — It Knows Something:"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fChest open — nearby chest opens on its own"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fSign text — nearby sign shows your stats"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fDeath sound — low pitch player hurt sound"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fSleep horror — run §f/fm test sleep§f for instructions"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Phase 3 — It Knows YOU:"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fFake join — your own join message appears"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fMemory book — book in inventory with your real data"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fPast block — cobblestone appears near you"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fIntel reveal — subtitle shows your flee HP or death coords"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Phase 4 — Confrontation:"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fObsessed stalks — entity spawns 60-80 blocks away"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fReplica base — copy of your blocks appears ~200 blocks away with sign"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fMutated chat — your past message shown corrupted under your name"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Combat:"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fRun §f/fm test combat §8— spawn TheOnlyOne, die to it"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fDeath book appears in inventory (exact stats)"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fDEFEAT ending fires (title + sound)"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fKill TheOnlyOne — it drops error log book"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fVICTORY ending fires"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fRun §f/fm falsevictory§8 to test false-victory window"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Visual effects (requires tier 2+):"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fVignette fades in at corners"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fStatic flickers randomly"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fManhunt border pulses red (§f/fm manhunt on§8)"));
        player.sendSystemMessage(Component.literal("  §8[ ] §fSleep: screen inverts on enter, shakes on wake"));
    }

    private static void dumpMemory(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) { player.sendSystemMessage(Component.literal("§c[DEBUG] No MemoryManager.")); return; }
        PlayerMemory m = mgr.getOrCreate(player);

        player.sendSystemMessage(Component.literal("§e[DEBUG] === MEMORY DUMP ==="));
        player.sendSystemMessage(Component.literal("§7Tier: §f" + m.knowledgeTier));
        player.sendSystemMessage(Component.literal("§7Day: §f" + m.worldDayCount));
        player.sendSystemMessage(Component.literal("§7False victory day: §f" + m.falseVictoryDay));
        player.sendSystemMessage(Component.literal("§7Deaths: §f" + m.totalDeaths));
        player.sendSystemMessage(Component.literal("§7Sleep count: §f" + m.sleepCount));
        player.sendSystemMessage(Component.literal("§7Biomes visited: §f" + m.visitedBiomes.size()));
        player.sendSystemMessage(Component.literal("§7Home pos: §f" + (m.inferredHomePos != null ? m.inferredHomePos : "unknown")));
        player.sendSystemMessage(Component.literal("§7Bed pos: §f" + (m.preferredBedPos != null ? m.preferredBedPos : "unknown")));
        player.sendSystemMessage(Component.literal("§7Weapon: §f" + m.preferredWeaponType));
        player.sendSystemMessage(Component.literal("§7Flee HP avg: §f" + String.format("%.1f", m.averageHealthWhenFleeing / 2f)));
        player.sendSystemMessage(Component.literal("§7Flee rate: §f" + String.format("%.0f%%", m.getCombatFleeRate() * 100)));
        player.sendSystemMessage(Component.literal("§7In manhunt: §f" + m.inManhunt));
        player.sendSystemMessage(Component.literal("§7Ticks played: §f" + m.totalTicksPlayed));
        player.sendSystemMessage(Component.literal("§7Events triggered: §f" + m.triggeredEvents.size()));
        player.sendSystemMessage(Component.literal("§7Chat history: §f" + m.chatHistory.size() + " entries"));
        player.sendSystemMessage(Component.literal("§7Mining Y (most): §f" + m.getMostCommonMiningY()));
        player.sendSystemMessage(Component.literal("§7Profile type: §f" + m.playerProfile.profileType));
        player.sendSystemMessage(Component.literal("§7Aggressivity: §f" + m.aggressivity.getCurrentLevel().name()));
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
                    player.sendSystemMessage(Component.literal("\u00a7a[DEBUG] Spawned The Obsessed."));
                }
            }
            case "onlyone" -> {
                TheOnlyOneEntity e = ModEntities.THE_ONLY_ONE.get().create(level);
                if (e != null) {
                    e.initFromPlayer(player);
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("\u00a7a[DEBUG] Spawned The Only One."));
                }
            }
            case "witness" -> {
                TheWitnessEntity e = ModEntities.THE_WITNESS.get().create(level);
                if (e != null) {
                    e.setTargetPlayer(player.getUUID());
                    e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(e);
                    player.sendSystemMessage(Component.literal("\u00a7a[DEBUG] Spawned The Witness."));
                }
            }
            case "lostmemories" -> {
                com.ryvione.falsememory.world.dimension.LostMemoriesDimension.enterDimension(player);
                player.sendSystemMessage(Component.literal("\u00a7a[DEBUG] Entering Lost Memories dimension."));
            }
            default -> player.sendSystemMessage(Component.literal(
                "\u00a7c[DEBUG] Unknown entity. Options: obsessed, onlyone, witness, lostmemories"));
        }
    }

    private static void triggerEvent(ServerPlayer player, String eventName) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        m.triggeredEvents.removeIf(e -> e.contains(eventName));

        switch (eventName.toLowerCase()) {
            case "whisper"      -> HorrorEvents.ambientWhisper(player, m);
            case "blockshift"   -> HorrorEvents.subtleBlockShift(player, level, m);
            case "torch"        -> HorrorEvents.missingTorch(player, level, m);
            case "footsteps"    -> HorrorEvents.fakeFootsteps(player, m);
            case "footsteps2"   -> HorrorEvents.fakeFootstepsMulti(player, m, 1);
            case "footsteps3"   -> HorrorEvents.fakeFootstepsMulti(player, m, 2);
            case "book"         -> HorrorEvents.memoryBookAppears(player, m);
            case "join"         -> HorrorEvents.fakePlayerJoinMessage(player, m);
            case "death"        -> HorrorEvents.yourOwnDeathSoundDistant(player, m);
            case "chat"         -> HorrorEvents.fakeChatMutated(player, m);
            case "replica"      -> HorrorEvents.replicaBaseEventCorrupted(player, level, m);
            case "stalks"       -> HorrorEvents.theObsessedStalks(player, level, m);
            case "echo"         -> HorrorEvents.timelineEcho(player, level, m);
            case "predictive"   -> HorrorEvents.predictivePlacement(player, level, m);
            case "invasion"     -> HorrorEvents.baseInvasion(player, level, m);
            case "intel"        -> HorrorEvents.intelligenceReveal(player, m);
            case "sign"         -> HorrorEvents.signTextChanged(player, level, m);
            case "chest"        -> HorrorEvents.chestSlightlyOpen(player, level, m);
            case "sleep"        -> HorrorEvents.sleepHorror(player, level, m);
            case "standing"     -> HorrorEvents.standingWhereYouStand(player, level, m);
            case "pastblock"    -> HorrorEvents.blockFromYourPastAppears(player, level, m);
            
            case "inventory"    -> HorrorEvents.inventoryObservation(player, m);
            case "session"      -> HorrorEvents.sessionTimeComment(player, m);
            case "helditem"     -> HorrorEvents.currentInventorySpook(player, m);
            case "4thwall"      -> HorrorEvents.fourthWallBreak(player, m);
            case "sysmsg"       -> HorrorEvents.falseSysMessage(player, m);
            case "watcher"      -> HorrorEvents.silentWatcher(player, level, m);
            case "prechchest"   -> HorrorEvents.predictiveChest(player, level, m);
            case "anticipate"   -> HorrorEvents.anticipatoryBlockPlacement(player, level, m);
            case "escalate"     -> HorrorEvents.escalatingNotes(player, level, m);
            case "echochat"     -> HorrorEvents.echoedChatCorruption(player, m);
            case "deathclone"   -> HorrorEvents.deathPositionClone(player, level, m);
            case "mining"       -> HorrorEvents.miningPresence(player, level, m);
            case "rooftop"      -> HorrorEvents.rooftopFootsteps(player, level, m);
            case "wakeroom"     -> HorrorEvents.wakeUpWrongRoom(player, level, m);
            default -> {
                player.sendSystemMessage(Component.literal(
                    "§c[DEBUG] Unknown event. Original: whisper, blockshift, torch, footsteps, footsteps2, footsteps3, book, join, death, chat, replica, stalks, echo, predictive, invasion, intel, sign, chest, sleep, standing, pastblock"));
                player.sendSystemMessage(Component.literal(
                    "§c[DEBUG] New: inventory, session, helditem, 4thwall, sysmsg, watcher, prechchest, anticipate, escalate, echochat, deathclone, mining, rooftop, wakeroom"));
                return;
            }
        }

        player.sendSystemMessage(Component.literal("§a[DEBUG] Triggered: " + eventName));
    }

    private static void spawnStructure(ServerPlayer player, String type) {
        if (!(player.level() instanceof ServerLevel level)) return;
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);
        BlockPos pos = player.blockPosition().offset(5, 0, 5);

        switch (type.toLowerCase()) {
            case "camp"    -> { AbandonedCampStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Abandoned Camp.")); }
            case "archive" -> { ArchiveStructure.spawn(level, pos, m);
                player.sendSystemMessage(Component.literal("§a[DEBUG] Spawned Archive.")); }
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown structure. Options: camp, archive"));
        }
    }

    private static void triggerEnding(ServerPlayer player, String type) {
        switch (type.toLowerCase()) {
            case "victory" -> EndingManager.triggerEnding(player, EndingType.VICTORY);
            case "defeat"  -> EndingManager.triggerEnding(player, EndingType.DEFEAT);
            case "draw"    -> EndingManager.triggerEnding(player, EndingType.DRAW);
            default -> { player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown ending. Options: victory, defeat, draw")); return; }
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
        m.falseVictoryDay = -1;
        m.totalDeaths = 0;
        m.deathPositions.clear();
        m.chatHistory.clear();
        m.loginPositions.clear();
        m.recentlyPlacedBlocks.clear();
        m.inferredHomePos = null;
        m.preferredBedPos = null;
        mgr.markDirty(player.getUUID());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.ryvione.falsememory.network.SanityPacket(0));
        player.sendSystemMessage(Component.literal("§a[DEBUG] Memory fully reset."));
    }

    private static void setManhunt(ServerPlayer player, boolean on) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        PlayerMemory m = mgr.getOrCreate(player);
        if (on) {
            TheOnlyOneEntity dummy = ModEntities.THE_ONLY_ONE.get().create(level);
            if (dummy != null) ManhuntManager.activate(player, level, dummy, m);
        } else {
            ManhuntManager.deactivate(player, m);
        }
        mgr.markDirty(player.getUUID());
        player.sendSystemMessage(Component.literal("§a[DEBUG] Manhunt: " + (on ? "ON" : "OFF")));
    }

    private static void listData(ServerPlayer player) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) { player.sendSystemMessage(Component.literal("§c[DEBUG] No MemoryManager.")); return; }
        PlayerMemory m = mgr.getOrCreate(player);

        player.sendSystemMessage(Component.literal("§6§l[FM DATA] ══════════════════════════"));
        
        player.sendSystemMessage(Component.literal("§e▸ Core"));
        player.sendSystemMessage(Component.literal("  §7Tier: §f" + m.knowledgeTier + "  §7Day: §f" + m.worldDayCount
            + "  §7Ticks: §f" + m.totalTicksPlayed));
        player.sendSystemMessage(Component.literal("  §7Deaths: §f" + m.totalDeaths
            + "  §7Last cause: §f" + (m.lastDeathCause != null ? m.lastDeathCause : "—")
            + "  §7Sleep count: §f" + m.sleepCount));
        player.sendSystemMessage(Component.literal("  §7FalseVictory day: §f" + (m.falseVictoryDay < 0 ? "—" : m.falseVictoryDay)));
        player.sendSystemMessage(Component.literal("  §7Manhunt: §f" + (m.inManhunt ? "§cON" : "§aOFF")));

        player.sendSystemMessage(Component.literal("§e▸ Location"));
        player.sendSystemMessage(Component.literal("  §7Home: §f" + (m.inferredHomePos != null ? m.inferredHomePos : "unknown")));
        player.sendSystemMessage(Component.literal("  §7Bed:  §f" + (m.preferredBedPos != null ? m.preferredBedPos : "unknown")));
        player.sendSystemMessage(Component.literal("  §7Login positions: §f" + m.loginPositions.size()));
        player.sendSystemMessage(Component.literal("  §7Death positions: §f" + m.deathPositions.size()));
        if (!m.deathPositions.isEmpty()) {
            BlockPos last = m.deathPositions.get(m.deathPositions.size() - 1);
            player.sendSystemMessage(Component.literal("    §8Last death: §7" + last));
        }

        player.sendSystemMessage(Component.literal("§e▸ Combat"));
        player.sendSystemMessage(Component.literal("  §7Weapon: §f" + (m.preferredWeaponType != null ? m.preferredWeaponType : "—")
            + "  §7Ranged: §f" + m.usesRangedWeapons + "  §7Potions: §f" + m.usesPotions));
        player.sendSystemMessage(Component.literal("  §7Combats: §f" + m.totalCombatEvents
            + " §7 Fled: §f" + m.combatsFled
            + " §7 Stood: §f" + m.combatsStoodGround
            + " §7 FleeHP: §f" + String.format("%.1f", m.averageHealthWhenFleeing / 2f)));
        if (!m.deathCauseHistory.isEmpty()) {
            StringBuilder causes = new StringBuilder();
            m.deathCauseHistory.forEach((k, v) -> causes.append(k).append("×").append(v).append(" "));
            player.sendSystemMessage(Component.literal("  §7Death causes: §f" + causes.toString().trim()));
        }

        player.sendSystemMessage(Component.literal("§e▸ World"));
        player.sendSystemMessage(Component.literal("  §7Biomes visited: §f" + m.visitedBiomes.size()
            + "  §7Freq biome: §f" + (m.mostFrequentBiome != null ? m.mostFrequentBiome : "—")));
        player.sendSystemMessage(Component.literal("  §7Blocks placed (types): §f" + m.blockPlacementCounts.size()
            + "  §7Broken: §f" + m.blockBreakCounts.size()
            + "  §7Recent placed: §f" + m.recentlyPlacedBlocks.size()));
        player.sendSystemMessage(Component.literal("  §7Mining Y (mode): §f" + m.getMostCommonMiningY()));

        player.sendSystemMessage(Component.literal("§e▸ AI Profile"));
        player.sendSystemMessage(Component.literal("  §7Profile: §f" + m.playerProfile.profileType
            + "  §7Aggressivity: §f" + m.aggressivity.getCurrentLevel().name()));
        player.sendSystemMessage(Component.literal("  §7Traps known: §f" + m.learnedTrapPatterns.size()
            + "  §7Detected traps: §f" + m.detectedTraps.size()));

        player.sendSystemMessage(Component.literal("§e▸ Inventory"));
        player.sendSystemMessage(Component.literal("  §7Crafted items (types): §f" + m.craftedItems.size()));
        if (!m.craftedItems.isEmpty()) {
            StringBuilder crafted = new StringBuilder();
            m.craftedItems.entrySet().stream().limit(5).forEach(e ->
                crafted.append(e.getKey().replace("minecraft:", "")).append("×").append(e.getValue()).append(" "));
            player.sendSystemMessage(Component.literal("    §8" + crafted.toString().trim()));
        }

        player.sendSystemMessage(Component.literal("§e▸ Events"));
        player.sendSystemMessage(Component.literal("  §7Triggered: §f" + m.triggeredEvents.size()));
        if (!m.triggeredEvents.isEmpty()) {
            player.sendSystemMessage(Component.literal("    §8" + String.join(", ",
                m.triggeredEvents.stream().limit(8).toList()) + (m.triggeredEvents.size() > 8 ? "…" : "")));
        }

        player.sendSystemMessage(Component.literal("§e▸ Chat"));
        player.sendSystemMessage(Component.literal("  §7Messages tracked: §f" + m.chatHistory.size()));
        if (!m.chatHistory.isEmpty()) {
            player.sendSystemMessage(Component.literal("    §8Last: \"" + m.chatHistory.get(m.chatHistory.size() - 1) + "\""));
        }
        player.sendSystemMessage(Component.literal("§6§l══════════════════════════════════"));
    }

    private static void handleDebugAction(ServerPlayer player, String action) {
        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory m = mgr.getOrCreate(player);

        switch (action.toLowerCase()) {
            case "obsessed_visible" -> {
                if (player.level() instanceof ServerLevel level) {
                    level.getEntities().getAll().forEach(e -> {
                        if (e instanceof TheObsessedEntity obs) obs.setInvisible(false);
                    });
                    player.sendSystemMessage(Component.literal("§a[DEBUG] Obsessed made visible."));
                }
            }
            case "obsessed_invisible" -> {
                if (player.level() instanceof ServerLevel level) {
                    level.getEntities().getAll().forEach(e -> {
                        if (e instanceof TheObsessedEntity obs) obs.setInvisible(true);
                    });
                    player.sendSystemMessage(Component.literal("§a[DEBUG] Obsessed made invisible."));
                }
            }
            case "clear_traps" -> {
                m.detectedTraps.clear();
                m.learnedTrapPatterns.clear();
                mgr.markDirty(player.getUUID());
                player.sendSystemMessage(Component.literal("§a[DEBUG] Trap data cleared."));
            }
            case "clear_events" -> {
                m.triggeredEvents.clear();
                mgr.markDirty(player.getUUID());
                player.sendSystemMessage(Component.literal("§a[DEBUG] Events cleared."));
            }
            case "clear_positions" -> {
                m.loginPositions.clear();
                m.deathPositions.clear();
                mgr.markDirty(player.getUUID());
                player.sendSystemMessage(Component.literal("§a[DEBUG] Positions cleared."));
            }
            default -> player.sendSystemMessage(Component.literal(
                "§c[DEBUG] Unknown action. Options: obsessed_visible, obsessed_invisible, clear_traps, clear_events, clear_positions"));
        }
    }

    private static void sendHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§e[DEBUG] === FALSE MEMORY DEBUG ==="));
        player.sendSystemMessage(Component.literal("§7/fm pin <code>§f - Unlock debug"));
        player.sendSystemMessage(Component.literal("§7/fm lock§f - Lock session"));
        player.sendSystemMessage(Component.literal("§7/fm seed§f - §eInject fake memory data (do this first)"));
        player.sendSystemMessage(Component.literal("§7/fm test <phase|all|combat|sleep>§f - §eRun test suite"));
        player.sendSystemMessage(Component.literal("§7/fm checklist§f - §eFull QA checklist"));
        player.sendSystemMessage(Component.literal("§7/fm falsevictory§f - §eTest false victory window"));
        player.sendSystemMessage(Component.literal("§7/fm memory§f - Dump all tracked data"));
        player.sendSystemMessage(Component.literal("§7/fm tier <0-3>§f - Set knowledge tier"));
        player.sendSystemMessage(Component.literal("§7/fm day <n>§f - Set tracked day"));
        player.sendSystemMessage(Component.literal("§7/fm event <name>§f - Trigger single event"));
        player.sendSystemMessage(Component.literal("§7/fm spawn <entity>§f - Spawn entity"));
        player.sendSystemMessage(Component.literal("§7/fm structure <type>§f - Spawn structure"));
        player.sendSystemMessage(Component.literal("§7/fm ending <type>§f - Trigger ending"));
        player.sendSystemMessage(Component.literal("§7/fm manhunt <on/off>§f - Toggle manhunt"));
        player.sendSystemMessage(Component.literal("§7/fm reset§f - Wipe all memory"));
        player.sendSystemMessage(Component.literal("§7/fm data§f - Quick data summary"));
    }

    private static void deny(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[DEBUG] Access denied. Use /fm pin <code> first."));
    }
}