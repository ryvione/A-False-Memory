package com.ryvione.falsememory.events;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.util.SoundUtil;
import com.ryvione.falsememory.util.TitleUtil;
import java.util.Map;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.entity.TheObsessedEntity;
import com.ryvione.falsememory.entity.TheOnlyOneEntity;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class HorrorEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG    = new Random();

    public static void subtleBlockShift(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("block_shift")) return;
        BlockPos center = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();
        for (int attempts = 0; attempts < 30; attempts++) {
            BlockPos pos = center.offset(RNG.nextInt(11) - 5, RNG.nextInt(5) - 2, RNG.nextInt(11) - 5);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK && level.getBlockState(pos.above()).isAir()) {
                BlockPos[] dirs = {pos.north(), pos.south(), pos.east(), pos.west()};
                BlockPos target = dirs[RNG.nextInt(4)];
                if (level.getBlockState(target).isAir()) {
                    level.setBlock(target, state, 3);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    memory.markTriggered("block_shift");
                    return;
                }
            }
        }
    }

    public static void ambientWhisper(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("whisper")) return;
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
            0.15f, 0.2f + RNG.nextFloat() * 0.15f);
        memory.markTriggered("whisper");
    }

    public static void missingTorch(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("missing_torch")) return;
        BlockPos center = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();
        for (int attempts = 0; attempts < 50; attempts++) {
            BlockPos pos = center.offset(RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                memory.markTriggered("missing_torch");
                return;
            }
        }
    }

    public static void fakeFootsteps(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_footsteps")) return;
        for (int i = 0; i < 4; i++) {
            playSoundForPlayer(player, "minecraft:block.stone.step", SoundSource.PLAYERS,
                0.4f + RNG.nextFloat() * 0.2f, 0.8f + RNG.nextFloat() * 0.4f);
        }
        memory.markTriggered("fake_footsteps");
    }

    public static void chestSlightlyOpen(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("chest_open")) return;
        BlockPos center = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();
        for (int attempts = 0; attempts < 60; attempts++) {
            BlockPos pos = center.offset(RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                level.blockEvent(pos, state.getBlock(), 1, 1);
                memory.markTriggered("chest_open");
                return;
            }
        }
    }

    public static void signTextChanged(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("sign_text")) return;
        BlockPos center = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();

        String[] lines = buildPersonalizedSignText(player, memory);

        for (int attempts = 0; attempts < 80; attempts++) {
            BlockPos pos = center.offset(RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.OAK_SIGN) || state.is(Blocks.OAK_WALL_SIGN)
                || state.is(Blocks.BIRCH_SIGN) || state.is(Blocks.SPRUCE_SIGN)) {
                var be = level.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                    for (int i = 0; i < 4; i++) {
                        sign.getFrontText().setMessage(i,
                            Component.literal(i < lines.length ? lines[i] : ""));
                    }
                    sign.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    memory.markTriggered("sign_text");
                    return;
                }
            }
        }
    }

    private static String[] buildPersonalizedSignText(ServerPlayer player, PlayerMemory memory) {
        int roll = RNG.nextInt(5);
        return switch (roll) {
            case 0 -> new String[]{ 
                "You flee at",
                String.format("%.1f hearts", memory.averageHealthWhenFleeing / 2f),
                "I know",
                "exactly when."
            };
            case 1 -> { 
                BlockPos lastDeath = memory.deathPositions.isEmpty()
                    ? null : memory.deathPositions.get(memory.deathPositions.size() - 1);
                yield lastDeath != null
                    ? new String[]{ "You died here.", "I watched.", lastDeath.getX() + " / " + lastDeath.getZ(), "Come back." }
                    : new String[]{ "I see you.", "", "Do you see", "me?" };
            }
            case 2 -> 
                memory.chatHistory.isEmpty()
                    ? new String[]{ "I hear you", "even when", "you are", "silent." }
                    : new String[]{ "You said:", memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()))
                        .substring(0, Math.min(15, memory.chatHistory.get(0).length())), "I remember", "everything." };
            case 3 -> { 
                String weapon = memory.preferredWeaponType != null ? memory.preferredWeaponType : "sword";
                yield new String[]{ "Your " + weapon, "will not", "be enough.", "" };
            }
            default -> { 
                String biome = memory.mostFrequentBiome != null
                    ? memory.mostFrequentBiome.replace("minecraft:", "").replace("_", " ") : "world";
                yield new String[]{ "I followed", "you through", "the " + biome, "." };
            }
        };
    }

    public static void fakePlayerJoinMessage(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_join")) return;
        Component msg = Component.translatable("multiplayer.player.joined",
            Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(msg);
        memory.markTriggered("fake_join");
    }

    public static void memoryBookAppears(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("memory_book")) return;

        List<String> rawPages = new java.util.ArrayList<>();

        String weaponStr = memory.preferredWeaponType != null ? memory.preferredWeaponType : "nothing";
        rawPages.add(
            "You fight with a " + weaponStr + ".\n\n" +
            "You run when you have\n" + String.format("%.1f", memory.averageHealthWhenFleeing / 2f) + " hearts left.\n\n" +
            (memory.usesPotions ? "You use potions.\nI will too." : "You don't use potions.\nYou should.")
        );

        if (memory.inferredHomePos != null) {
            BlockPos home = memory.inferredHomePos;
            rawPages.add(
                "You always come back\nto " + home.getX() + ", " + home.getZ() + ".\n\n" +
                "I have been there.\n\nI will be there\nwhen you return."
            );
        }

        if (!memory.deathPositions.isEmpty()) {
            BlockPos lastDeath = memory.deathPositions.get(memory.deathPositions.size() - 1);
            String deathCause = memory.getMostFrequentDeathCause();
            rawPages.add(
                "You have died " + memory.totalDeaths + " time" + (memory.totalDeaths == 1 ? "" : "s") + ".\n\n" +
                (deathCause != null ? "Most often to:\n" + deathCause.replace("_", " ") + ".\n\n" : "") +
                "Your last death:\n" + lastDeath.getX() + " " + lastDeath.getY() + " " + lastDeath.getZ()
            );
        }

        rawPages.add(
            "You always face\n" + getDirectionName(memory.getDominantFacingYaw()) + ".\n\n" +
            "That is why you\nnever see me coming."
        );

        if (!memory.chatHistory.isEmpty()) {
            String msg = memory.chatHistory.get(memory.chatHistory.size() - 1);
            rawPages.add(
                "Last words:\n\n\"" + msg.substring(0, Math.min(40, msg.length())) + "\"\n\nI was listening."
            );
        }

        ItemStack book = com.ryvione.falsememory.util.BookUtil.createWrittenBook("Observations", "???", rawPages);

        if (!player.getInventory().add(book)) player.drop(book, false);
        com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
            com.ryvione.falsememory.advancement.AdvancementTriggers.MEMORY_BOOK);
        memory.markTriggered("memory_book");
    }

    public static void yourOwnDeathSoundDistant(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("death_sound_distant")) return;
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
            0.4f, 0.3f + RNG.nextFloat() * 0.15f);
        memory.markTriggered("death_sound_distant");
    }

    public static void blockFromYourPastAppears(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("past_block_appears")) return;
        if (memory.recentlyPlacedBlocks.isEmpty()) return;
        BlockPos playerPos = player.blockPosition();
        for (int attempts = 0; attempts < 20; attempts++) {
            BlockPos target = playerPos.offset(RNG.nextInt(9) - 4, 0, RNG.nextInt(9) - 4);
            if (level.getBlockState(target).isAir() && !level.getBlockState(target.below()).isAir()) {
                level.setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
                memory.markTriggered("past_block_appears");
                return;
            }
        }
    }

    public static void baseInvasion(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("base_invasion")) return;
        if (memory.inferredHomePos == null) return;
        boolean obsessedExists = !level.getEntities(ModEntities.THE_OBSESSED.get(),
            e -> e instanceof TheObsessedEntity obs &&
                player.getUUID().toString().equals(obs.getTargetPlayerUUID())).isEmpty();
        if (!obsessedExists) {
            BlockPos home = memory.inferredHomePos;
            TheObsessedEntity obsessed = ModEntities.THE_OBSESSED.get().create(level);
            if (obsessed != null) {
                obsessed.setTargetPlayer(player.getUUID());
                obsessed.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                level.addFreshEntity(obsessed);
            }
        }
        sendTitle(player, "", "...", 5, 40, 20);
        playSoundForPlayer(player, "minecraft:entity.enderman.ambient", SoundSource.HOSTILE, 0.3f, 0.6f);
        memory.markTriggered("base_invasion");
    }

    public static void timelineEcho(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("timeline_echo")) return;
        if (memory.loginPositions.size() < 2) return;
        BlockPos pastPos = memory.loginPositions.get(memory.loginPositions.size() - 2);
        double dist = player.blockPosition().distSqr(pastPos);
        if (dist < 100 || dist > 90000) return;
        level.levelEvent(2003, pastPos, 0);
        playSoundForPlayer(player, "minecraft:entity.enderman.teleport", SoundSource.HOSTILE, 0.4f, 0.5f);
        sendTitle(player, "", "It was just here.", 5, 40, 20);
        memory.markTriggered("timeline_echo");
    }

    public static void theObsessedStalks(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("obsessed_stalks")) return;
        boolean exists = !level.getEntities(ModEntities.THE_OBSESSED.get(),
            e -> e instanceof TheObsessedEntity obs &&
                player.getUUID().toString().equals(obs.getTargetPlayerUUID())).isEmpty();
        if (!exists) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist  = 60 + RNG.nextInt(20);
            BlockPos spawnPos = BlockPos.containing(
                player.getX() + Math.sin(angle) * dist, player.getY(),
                player.getZ() + Math.cos(angle) * dist);
            while (level.getBlockState(spawnPos).isAir() && spawnPos.getY() > 0) spawnPos = spawnPos.below();
            spawnPos = spawnPos.above();
            TheObsessedEntity obsessed = ModEntities.THE_OBSESSED.get().create(level);
            if (obsessed != null) {
                obsessed.setTargetPlayer(player.getUUID());
                obsessed.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(obsessed);
            }
        }
        sendTitle(player, "", "...", 10, 40, 20);
        playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.3f, 0.5f);
        memory.markTriggered("obsessed_stalks");
    }

    public static void replicaBaseEvent(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("replica_base")) return;
        if (memory.recentlyPlacedBlocks.size() < 10) return;
        BlockPos home = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();
        int offsetX = (150 + RNG.nextInt(50)) * (RNG.nextBoolean() ? 1 : -1);
        int offsetZ = (150 + RNG.nextInt(50)) * (RNG.nextBoolean() ? 1 : -1);
        BlockPos replicaOrigin = new BlockPos(home.getX() + offsetX, home.getY(), home.getZ() + offsetZ);
        List<BlockPos> blocks = memory.recentlyPlacedBlocks.subList(
            Math.max(0, memory.recentlyPlacedBlocks.size() - 20),
            memory.recentlyPlacedBlocks.size());
        int ax = 0, ay = 0, az = 0;
        for (BlockPos b : blocks) { ax += b.getX(); ay += b.getY(); az += b.getZ(); }
        ax /= blocks.size(); ay /= blocks.size(); az /= blocks.size();
        final int fx = ax, fy = ay, fz = az;
        for (BlockPos orig : blocks) {
            BlockState state = level.getBlockState(orig);
            if (!state.isAir()) {
                level.setBlock(new BlockPos(
                    replicaOrigin.getX() + (orig.getX() - fx),
                    replicaOrigin.getY() + (orig.getY() - fy),
                    replicaOrigin.getZ() + (orig.getZ() - fz)), state, 3);
            }
        }
        memory.markTriggered("replica_base");
    }

    public static void fakeChatFromPast(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_chat")) return;
        if (memory.chatHistory.isEmpty()) return;
        String past = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
        player.sendSystemMessage(Component.literal("??? ")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(past).withStyle(ChatFormatting.GRAY)));
        memory.markTriggered("fake_chat");
    }

    public static void standingWhereYouStand(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("standing_where_you_stand")) return;
        if (memory.loginPositions.size() < 2) return;
        BlockPos last = memory.loginPositions.get(memory.loginPositions.size() - 2);
        double dist = player.blockPosition().distSqr(last);
        if (dist < 400 || dist > 40000) return;
        level.levelEvent(2003, last, 0);
        playSoundForPlayer(player, "minecraft:entity.enderman.teleport", SoundSource.HOSTILE, 0.2f, 0.7f);
        sendTitle(player, "", "...", 5, 30, 15);
        memory.markTriggered("standing_where_you_stand");
    }

    public static void predictivePlacement(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("predictive_placement")) return;
        String hotbarItem = memory.lastHotbarSnapshot.isEmpty() ? null : memory.lastHotbarSnapshot.get(0);
        if (hotbarItem != null) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(hotbarItem)).ifPresent(item -> {
                ItemStack stack = new ItemStack(item);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            });
        }
        sendTitle(player, "", "I already know.", 5, 40, 20);
        playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.2f, 0.4f);
        memory.markTriggered("predictive_placement");
    }

    public static void intelligenceReveal(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("intel_reveal")) return;

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;

        int roll = RNG.nextInt(4);
        switch (roll) {
            case 0 -> {
                float fleeHP = memory.averageHealthWhenFleeing / 2f;
                sendTitle(player, "", String.format("%.1f hearts. I remember.", fleeHP), 10, 70, 20);
            }
            case 1 -> {
                if (!memory.deathPositions.isEmpty()) {
                    BlockPos dp = memory.deathPositions.get(memory.deathPositions.size() - 1);
                    sendTitle(player, "", dp.getX() + " " + dp.getY() + " " + dp.getZ(), 10, 70, 20);
                }
            }
            case 2 -> sendTitle(player, "",
                "You always face " + getDirectionName(memory.getDominantFacingYaw()), 10, 70, 20);
            case 3 -> {
                if (!memory.chatHistory.isEmpty()) {
                    String msg = memory.chatHistory.get(memory.chatHistory.size() - 1);
                    sendTitle(player, "", "\"" + msg.substring(0, Math.min(20, msg.length())) + "\"", 10, 70, 20);
                }
            }
        }
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
            0.4f, 0.2f + RNG.nextFloat() * 0.1f);
        memory.markTriggered("intel_reveal");
    }

    public static void loginAmbience(ServerPlayer player, PlayerMemory memory) {
        if (player.level().getDayTime() % 24000 < 13000) {
            playSoundForPlayer(player, "minecraft:ambient.cave", SoundSource.AMBIENT, 0.15f, 0.4f);
        } else {
            playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.4f, 0.6f);
            sendTitle(player, "", "It remembers.", 10, 40, 20);
        }
    }

    public static void manhuntLoginAmbience(ServerPlayer player, PlayerMemory memory) {
        sendTitle(player, "§4It knows where you are.", "§8It's coming.", 10, 60, 20);
        playSoundForPlayer(player, "minecraft:entity.wither.ambient", SoundSource.HOSTILE, 0.5f, 0.3f);
    }

    public static void playSoundForPlayer(ServerPlayer player, String soundId, SoundSource source,
                                           float volume, float pitch) {
        SoundUtil.playForPlayer(player, soundId, source, volume, pitch);
    }

    public static void sendTitle(ServerPlayer player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        TitleUtil.send(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void fakeFootstepsMulti(ServerPlayer player, PlayerMemory memory, int index) {
        int stepCount = 3 + RNG.nextInt(5);
        for (int i = 0; i < stepCount; i++) {
            float pitch = 0.6f + RNG.nextFloat() * 0.6f;
            playSoundForPlayer(player, "minecraft:block.stone.step", SoundSource.PLAYERS,
                0.5f + RNG.nextFloat() * 0.3f, pitch);
        }
        if (index >= 1) {
            playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
                0.2f, 0.25f + RNG.nextFloat() * 0.1f);
        }
    }

    public static void sleepHorror(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("sleep_horror")) return;
        if (memory.preferredBedPos == null) return;

        BlockPos bed = memory.preferredBedPos;
        for (int attempts = 0; attempts < 20; attempts++) {
            BlockPos near = bed.offset(RNG.nextInt(5) - 2, 0, RNG.nextInt(5) - 2);
            if (near.equals(bed)) continue;
            BlockState state = level.getBlockState(near);
            if (!state.isAir()) {
                BlockPos moved = near.offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
                if (level.getBlockState(moved).isAir()) {
                    level.setBlock(moved, state, 3);
                    level.setBlock(near, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }

        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
            0.3f, 0.2f);
        memory.markTriggered("sleep_horror");
    }

    public static void fakeChatMutated(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_chat")) return;
        if (memory.chatHistory.isEmpty()) return;

        String past = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
        String mutated = mutateChatMessage(past);

        player.sendSystemMessage(Component.literal("§8[" + player.getName().getString() + "] §7" + mutated));
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS,
            0.2f, 0.3f);
        memory.markTriggered("fake_chat");
    }

    private static String mutateChatMessage(String original) {
        if (original.isEmpty()) return "...";
        String lower = original.toLowerCase();
        int roll = RNG.nextInt(6);
        return switch (roll) {
            case 0 -> {
                int cut = original.length() / 2 + RNG.nextInt(Math.max(1, original.length() / 2));
                yield original.substring(0, Math.min(cut, original.length())) + "—";
            }
            case 1 -> {
                String[] words = original.split(" ");
                if (words.length <= 1) yield "...";
                yield words[words.length - 1];
            }
            case 2 -> original.replace("i", "it").replace("me", "you").replace("my", "your");
            case 3 -> {
                StringBuilder sb = new StringBuilder(original);
                for (int i = 0; i < 3; i++) {
                    int pos = RNG.nextInt(sb.length());
                    sb.setCharAt(pos, (char)('a' + RNG.nextInt(26)));
                }
                yield sb.toString();
            }
            case 4 -> original + " " + original.substring(0, Math.min(4, original.length()));
            default -> original.substring(0, Math.min(original.length(), 10)) + "...";
        };
    }

    public static void replicaBaseEventCorrupted(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("replica_base")) return;
        if (memory.recentlyPlacedBlocks.size() < 10) return;
        BlockPos home = memory.inferredHomePos != null ? memory.inferredHomePos : player.blockPosition();
        int offsetX = (150 + RNG.nextInt(50)) * (RNG.nextBoolean() ? 1 : -1);
        int offsetZ = (150 + RNG.nextInt(50)) * (RNG.nextBoolean() ? 1 : -1);
        BlockPos replicaOrigin = new BlockPos(home.getX() + offsetX, home.getY(), home.getZ() + offsetZ);
        List<BlockPos> blocks = memory.recentlyPlacedBlocks.subList(
            Math.max(0, memory.recentlyPlacedBlocks.size() - 20),
            memory.recentlyPlacedBlocks.size());
        int ax = 0, ay = 0, az = 0;
        for (BlockPos b : blocks) { ax += b.getX(); ay += b.getY(); az += b.getZ(); }
        ax /= blocks.size(); ay /= blocks.size(); az /= blocks.size();
        final int fx = ax, fy = ay, fz = az;
        for (BlockPos orig : blocks) {
            BlockState state = level.getBlockState(orig);
            if (!state.isAir()) {
                boolean corrupt = RNG.nextInt(8) == 0;
                BlockState placed = corrupt ? Blocks.NETHERRACK.defaultBlockState() : state;
                level.setBlock(new BlockPos(
                    replicaOrigin.getX() + (orig.getX() - fx),
                    replicaOrigin.getY() + (orig.getY() - fy),
                    replicaOrigin.getZ() + (orig.getZ() - fz)), placed, 3);
            }
        }

        BlockPos signPos = replicaOrigin.above(2);
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        var be = level.getBlockEntity(signPos);
        if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            sign.getFrontText().setMessage(0, Component.literal("I know this"));
            sign.getFrontText().setMessage(1, Component.literal("place."));
            sign.getFrontText().setMessage(2, Component.literal("Do you?"));
            sign.setChanged();
            level.sendBlockUpdated(signPos, Blocks.OAK_SIGN.defaultBlockState(),
                Blocks.OAK_SIGN.defaultBlockState(), 3);
        }

        playSoundForPlayer(player, "minecraft:entity.enderman.teleport", SoundSource.HOSTILE,
            0.4f, 0.4f);
        memory.markTriggered("replica_base");
    }

    public static void inventoryObservation(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("inventory_observation")) return;

        long checks = memory.totalInventoryChecks;
        long avgInterval = memory.averageInventoryCheckInterval;

        String[] messages;
        if (checks > 10 && avgInterval < 30) {
            messages = new String[]{
                "You've checked your inventory " + checks + " times.",
                "You're nervous.",
                "Good."
            };
        } else if (checks > 5) {
            messages = new String[]{
                "You opened your inventory " + checks + " times.",
                "Looking for something?",
                "It isn't there."
            };
        } else {
            messages = new String[]{
                "I know what you're carrying.",
                "I know what's missing.",
                "I took it."
            };
        }

        sendTitle(player, "", messages[RNG.nextInt(messages.length)], 10, 60, 25);
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS, 0.2f, 0.18f + RNG.nextFloat() * 0.08f);
        memory.markTriggered("inventory_observation");
    }

    public static void sessionTimeComment(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("session_time_comment")) return;

        long ticks = memory.totalTicksPlayed;
        long inGameMinutes = (ticks / 20) / 60;
        long inGameHours = inGameMinutes / 60;

        java.time.LocalTime now = java.time.LocalTime.now();
        int hour = now.getHour();

        String timeMsg;
        if (hour >= 0 && hour < 5) {
            timeMsg = "It's past midnight.\nYou should sleep.\n\nI'll still be here\nwhen you wake up.";
        } else if (hour >= 22) {
            timeMsg = "Late night.\nAlone.\n\nNot really, though.";
        } else if (inGameHours >= 3) {
            timeMsg = "You've been playing\nfor a while now.\n\nI've been watching\nthe whole time.";
        } else {
            timeMsg = "You just got here.\n\nSo did I.";
        }

        sendTitle(player, "", timeMsg.split("\n")[0], 10, 70, 25);
        playSoundForPlayer(player, "minecraft:ambient.cave", SoundSource.AMBIENT, 0.2f, 0.35f);
        memory.markTriggered("session_time_comment");
    }

    public static void currentInventorySpook(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("current_inventory_spook")) return;

        String heldItem = player.getMainHandItem().isEmpty()
            ? "nothing"
            : player.getMainHandItem().getItem().toString().replace("item.minecraft.", "").replace("_", " ");

        int hotbarCount = 0;
        for (int i = 0; i < 9; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) hotbarCount++;
        }

        String[] variants = {
            "You're holding a " + heldItem + ".\nI see it.",
            "You have " + hotbarCount + " items ready.\nNot enough.",
            "Put away the " + heldItem + ".\nYou won't need it.",
            "The " + heldItem + " won't protect you."
        };

        sendTitle(player, "", variants[RNG.nextInt(variants.length)].split("\n")[0], 10, 60, 20);
        playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.25f, 0.45f);
        memory.markTriggered("current_inventory_spook");
    }

    public static void predictiveChest(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("predictive_chest")) return;
        BlockPos playerPos = player.blockPosition();

        for (int attempts = 0; attempts < 30; attempts++) {
            BlockPos pos = playerPos.offset(RNG.nextInt(7) - 3, 0, RNG.nextInt(7) - 3);
            if (!pos.equals(playerPos) && level.getBlockState(pos).isAir()
                    && !level.getBlockState(pos.below()).isAir()) {

                level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
                var be = level.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    
                    int slot = 0;
                    for (Map.Entry<String, Integer> entry : memory.craftedItems.entrySet()) {
                        if (slot >= 3) break;
                        try {
                            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getOptional(net.minecraft.resources.ResourceLocation.parse(entry.getKey()));
                            item.ifPresent(i -> {
                                
                                ItemStack stack = new ItemStack(i, 1);
                                chest.setItem(RNG.nextInt(27), stack);
                            });
                        } catch (Exception ignored) {}
                        slot++;
                    }

                    ItemStack note = new ItemStack(Items.WRITTEN_BOOK);
                    String[] noteTexts = {
                        "I knew you'd need these.\n\nI always know\nwhat comes next.\n\nDo you?",
                        "These were yours once.\n\nNow they're mine.\n\nTake them.\n\nI don't need them anymore.",
                        "You were going to craft these today.\n\nI saved you the trouble.",
                    };
                    note = com.ryvione.falsememory.util.BookUtil.createSinglePageBook(
                        "A note", "???", noteTexts[RNG.nextInt(noteTexts.length)]
                    );
                    chest.setItem(26, note);
                    chest.setChanged();
                }
                playSoundForPlayer(player, "minecraft:block.chest.open", SoundSource.BLOCKS, 0.5f, 0.6f);
                sendTitle(player, "", "...", 5, 30, 10);
                memory.markTriggered("predictive_chest");
                return;
            }
        }
    }

    public static void anticipatoryBlockPlacement(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("anticipatory_block")) return;
        if (memory.recentlyPlacedBlocks.isEmpty()) return;

        String mostPlaced = memory.blockPlacementCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("minecraft:cobblestone");

        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad) * 5);
        int dz = (int) Math.round(Math.cos(rad) * 5);
        BlockPos ahead = player.blockPosition().offset(dx, 0, dz);

        while (level.getBlockState(ahead).isAir() && ahead.getY() > level.getMinBuildHeight()) {
            ahead = ahead.below();
        }
        ahead = ahead.above();

        BlockPos finalPos = ahead;

        if (level.getBlockState(finalPos).isAir()) {
        try {
        var optionalBlock = BuiltInRegistries.BLOCK
                .getOptional(ResourceLocation.parse(mostPlaced));
        optionalBlock.ifPresent(block -> {
            level.setBlock(finalPos, block.defaultBlockState(), 3);
            playSoundForPlayer(player, "minecraft:block.stone.place", SoundSource.BLOCKS, 0.4f, 0.9f);
        });
       } catch (Exception ignored) {}
}

        sendTitle(player, "", "I know where you're going.", 5, 50, 20);
        memory.markTriggered("anticipatory_block");
    }

    public static void silentWatcher(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("silent_watcher")) return;

        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 48 + RNG.nextInt(8); 
        BlockPos spawnPos = BlockPos.containing(
            player.getX() + Math.sin(angle) * dist,
            player.getY(),
            player.getZ() + Math.cos(angle) * dist
        );
        
        while (level.getBlockState(spawnPos).isAir() && spawnPos.getY() > level.getMinBuildHeight())
            spawnPos = spawnPos.below();
        spawnPos = spawnPos.above();

        TheObsessedEntity watcher = ModEntities.THE_OBSESSED.get().create(level);
        if (watcher != null) {
            watcher.setTargetPlayer(player.getUUID());
            watcher.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            double dx = player.getX() - spawnPos.getX();
            double dz = player.getZ() - spawnPos.getZ();
            watcher.setYRot((float)(Math.toDegrees(Math.atan2(-dx, dz))));
            
            watcher.setInvisible(false);
            level.addFreshEntity(watcher);

            memory.triggeredEvents.add("watcher_entity_" + watcher.getId());

            playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.15f, 0.7f);
        }
        memory.markTriggered("silent_watcher");
    }

    public static void silentWatcherDespawn(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        
        level.getEntities(ModEntities.THE_OBSESSED.get(), e ->
            e instanceof TheObsessedEntity obs &&
            player.getUUID().toString().equals(obs.getTargetPlayerUUID()))
            .forEach(e -> {
                String key = "watcher_entity_" + e.getId();
                if (memory.triggeredEvents.contains(key)) {
                    e.discard();
                    memory.triggeredEvents.remove(key);
                }
            });
        sendTitle(player, "", "It was just here.", 5, 40, 20);
    }

    public static void wakeUpWrongRoom(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.preferredBedPos == null) return;
        BlockPos bed = memory.preferredBedPos;

        int roll = RNG.nextInt(4);

        switch (roll) {
            case 0 -> {
                
                for (int attempts = 0; attempts < 30; attempts++) {
                    BlockPos pos = bed.offset(RNG.nextInt(7) - 3, RNG.nextInt(3) - 1, RNG.nextInt(7) - 3);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        playSoundForPlayer(player, "minecraft:block.fire.extinguish", SoundSource.BLOCKS, 0.4f, 0.8f);
                        return;
                    }
                }
            }
            case 1 -> {
                
                for (int attempts = 0; attempts < 30; attempts++) {
                    BlockPos pos = bed.offset(RNG.nextInt(5) - 2, 0, RNG.nextInt(5) - 2);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                        BlockPos[] dirs = {pos.north(), pos.south(), pos.east(), pos.west()};
                        for (BlockPos dir : dirs) {
                            if (level.getBlockState(dir).isAir()) {
                                level.setBlock(dir, state, 3);
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                return;
                            }
                        }
                    }
                }
            }
            case 2 -> {
                
                for (int attempts = 0; attempts < 30; attempts++) {
                    BlockPos pos = bed.offset(RNG.nextInt(7) - 3, RNG.nextInt(3) - 1, RNG.nextInt(7) - 3);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                        level.blockEvent(pos, state.getBlock(), 1, 1);
                        playSoundForPlayer(player, "minecraft:block.chest.open", SoundSource.BLOCKS, 0.5f, 0.7f);
                        return;
                    }
                }
            }
            case 3 -> {
                
                for (int attempts = 0; attempts < 30; attempts++) {
                    BlockPos pos = bed.offset(RNG.nextInt(7) - 3, RNG.nextInt(3) - 1, RNG.nextInt(7) - 3);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(net.minecraft.tags.BlockTags.ALL_SIGNS)) {
                        var be = level.getBlockEntity(pos);
                        if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                            String[] msgs = {"I watched", "you sleep.", "All night.", "I was here."};
                            for (int i = 0; i < 4; i++) sign.getFrontText().setMessage(i, Component.literal(msgs[i]));
                            sign.setChanged();
                            level.sendBlockUpdated(pos, state, state, 3);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void fourthWallBreak(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fourth_wall")) return;

        String name = player.getName().getString();
        java.time.LocalTime now = java.time.LocalTime.now();
        int hour = now.getHour();

        String[] variants;

        if (hour >= 0 && hour < 5) {
            variants = new String[]{
                name + ". Go to bed.",
                "It's 3am, " + name + ".",
                "You're still here.",
                "I'm still here too."
            };
        } else if (hour >= 22) {
            variants = new String[]{
                "Getting late, " + name + ".",
                name + ". Close the game.",
                "Don't play alone at night.",
                "I'm more active after dark."
            };
        } else {
            variants = new String[]{
                name + ".",
                "I know your name, " + name + ".",
                "Hello, " + name + ".",
                name + ". I see you."
            };
        }

        String msg = variants[RNG.nextInt(variants.length)];
        sendTitle(player, "", msg, 10, 80, 30);

        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS, 0.3f, 0.15f);
        memory.markTriggered("fourth_wall");
    }

    public static void falseSysMessage(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("false_sys_message")) return;

        String name = player.getName().getString();
        long checks = memory.totalInventoryChecks;
        long avgInterval = memory.averageInventoryCheckInterval;

        String[] messages = {
            "§8[System] §7" + name + " has been observed.",
            "§8[System] §7Behavioral data updated for " + name + ".",
            "§8[Memory] §7Pattern match: 94% confidence on " + name + ".",
            "§8[Warning] §7Entity ??? is tracking " + name + ". This is normal.",
            "§8[Log] §7" + name + " checked inventory " + checks + " times this session. Interval: §f" + avgInterval + "s§7.",
            "§8[System] §7Session duration exceeds recommended limit for " + name + ".",
        };

        player.sendSystemMessage(Component.literal(messages[RNG.nextInt(messages.length)]));
        memory.markTriggered("false_sys_message");
    }

    public static void escalatingNotes(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        int stage = 0;
        if (memory.triggeredEvents.contains("escalating_note_1")) stage = 1;
        if (memory.triggeredEvents.contains("escalating_note_2")) stage = 2;
        if (memory.triggeredEvents.contains("escalating_note_3")) return;

        if (memory.inferredHomePos == null) return;
        BlockPos home = memory.inferredHomePos;

        BlockPos notePos = null;
        for (int attempts = 0; attempts < 40; attempts++) {
            BlockPos pos = home.offset(RNG.nextInt(11) - 5, 0, RNG.nextInt(11) - 5);
            if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
                notePos = pos;
                break;
            }
        }
        if (notePos == null) return;

        final BlockPos finalNotePos = notePos;

        switch (stage) {
            case 0 -> {
                
                level.setBlock(finalNotePos, Blocks.OAK_SIGN.defaultBlockState(), 3);
                var be = level.getBlockEntity(finalNotePos);
                if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                    sign.getFrontText().setMessage(0, Component.literal("I found"));
                    sign.getFrontText().setMessage(1, Component.literal("your base."));
                    sign.getFrontText().setMessage(2, Component.literal("Nice place."));
                    sign.getFrontText().setMessage(3, Component.literal(""));
                    sign.setChanged();
                    level.sendBlockUpdated(finalNotePos, Blocks.OAK_SIGN.defaultBlockState(),
                        Blocks.OAK_SIGN.defaultBlockState(), 3);
                }
                memory.triggeredEvents.add("escalating_note_1");
            }
            case 1 -> {
                
                level.setBlock(finalNotePos, Blocks.OAK_SIGN.defaultBlockState(), 3);
                var be = level.getBlockEntity(finalNotePos);
                if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                    sign.getFrontText().setMessage(0, Component.literal("I was inside"));
                    sign.getFrontText().setMessage(1, Component.literal("while you slept."));
                    sign.getFrontText().setMessage(2, Component.literal("You never knew."));
                    sign.getFrontText().setMessage(3, Component.literal(""));
                    sign.setChanged();
                    level.sendBlockUpdated(finalNotePos, Blocks.OAK_SIGN.defaultBlockState(),
                        Blocks.OAK_SIGN.defaultBlockState(), 3);
                }
                playSoundForPlayer(player, "minecraft:entity.enderman.ambient", SoundSource.HOSTILE, 0.3f, 0.5f);
                memory.triggeredEvents.add("escalating_note_2");
            }
            case 2 -> {
                
                level.setBlock(finalNotePos, Blocks.CHEST.defaultBlockState(), 3);
                var be = level.getBlockEntity(finalNotePos);
                if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    String weaponStr = memory.preferredWeaponType != null ? memory.preferredWeaponType : "sword";
                    String deathCoords = memory.deathPositions.isEmpty() ? "unknown" :
                        memory.deathPositions.get(memory.deathPositions.size() - 1).toString();

                    ItemStack book = com.ryvione.falsememory.util.BookUtil.createSinglePageBook(
                        "Final Report", "???",
                        "I know everything.\n\nYou fight with a " + weaponStr + ".\nYou flee at " +
                            String.format("%.1f", memory.averageHealthWhenFleeing / 2f) + " hearts.\n\nYou died at:\n" + deathCoords + "\n\nI was there."
                    );
                    chest.setItem(13, book);
                    chest.setChanged();
                }
                sendTitle(player, "§4It was here.", "§8It knows everything.", 15, 80, 30);
                playSoundForPlayer(player, "minecraft:entity.wither.ambient", SoundSource.HOSTILE, 0.5f, 0.4f);
                memory.triggeredEvents.add("escalating_note_3");
            }
        }
    }

    public static void echoedChatCorruption(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("echoed_chat_corruption")) return;
        if (memory.chatHistory.isEmpty()) return;

        String name = player.getName().getString();
        String original = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));

        String corrupted = original
            .replace("i ", "you ")
            .replace("I ", "you ")
            .replace("my ", "your ")
            .replace("me ", "you ")
            .replace("need", "needed")
            .replace("find", "found")
            .replace("kill", "lost to")
            .replace("going", "gone");

        String[] suffixes = {
            " already.",
            " — I did it first.",
            " I remember.",
            " before you.",
            " too late."
        };

        player.sendSystemMessage(Component.literal(
            "§7<" + name + "> §8" + corrupted + suffixes[RNG.nextInt(suffixes.length)]));
        playSoundForPlayer(player, "minecraft:entity.player.hurt", SoundSource.PLAYERS, 0.3f, 0.25f);
        memory.markTriggered("echoed_chat_corruption");
    }

    public static void deathPositionClone(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("death_pos_clone")) return;
        if (memory.deathPositions.isEmpty()) return;

        BlockPos deathPos = memory.deathPositions.get(memory.deathPositions.size() - 1);
        double dist = player.blockPosition().distSqr(deathPos);
        
        if (dist > 40000 || dist < 100) return;

        TheObsessedEntity clone = ModEntities.THE_OBSESSED.get().create(level);
        if (clone != null) {
            clone.setTargetPlayer(player.getUUID());
            clone.moveTo(deathPos.getX() + 0.5, deathPos.getY(), deathPos.getZ() + 0.5);
            clone.setInvisible(false);
            
            double dx = player.getX() - deathPos.getX();
            double dz = player.getZ() - deathPos.getZ();
            clone.setYRot((float)(Math.toDegrees(Math.atan2(-dx, dz))));
            level.addFreshEntity(clone);
        }

        sendTitle(player, "", "You died here.", 10, 60, 25);
        playSoundForPlayer(player, "minecraft:entity.player.death", SoundSource.PLAYERS, 0.3f, 0.5f);
        memory.markTriggered("death_pos_clone");
    }

    public static void miningPresence(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("mining_presence")) return;
        
        if (player.getY() > 30) return;

        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        
        int dx = (int) Math.round(Math.sin(rad) * 2);
        int dz = (int) Math.round(-Math.cos(rad) * 2);
        BlockPos behindPos = player.blockPosition().offset(dx, 0, dz);

        if (level.getBlockState(behindPos).isAir() && !level.getBlockState(behindPos.below()).isAir()) {
            level.setBlock(behindPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            playSoundForPlayer(player, "minecraft:block.stone.place", SoundSource.BLOCKS, 0.6f, 0.9f);
            
            memory.markTriggered("mining_presence");
        }
    }

    public static void rooftopFootsteps(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("rooftop_footsteps")) return;
        if (memory.inferredHomePos == null) return;
        
        long time = level.getDayTime() % 24000;
        if (time < 13000) return;

        BlockPos home = memory.inferredHomePos;
        double dist = player.blockPosition().distSqr(home);
        if (dist > 900) return; 

        for (int i = 0; i < 5 + RNG.nextInt(4); i++) {
            playSoundForPlayer(player, "minecraft:block.stone.step", SoundSource.PLAYERS,
                0.5f + RNG.nextFloat() * 0.2f, 0.85f + RNG.nextFloat() * 0.2f);
        }
        memory.markTriggered("rooftop_footsteps");
    }

    private static String getDirectionName(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 22.5 || yaw >= 337.5) return "North";
        if (yaw < 67.5)  return "Northeast";
        if (yaw < 112.5) return "East";
        if (yaw < 157.5) return "Southeast";
        if (yaw < 202.5) return "South";
        if (yaw < 247.5) return "Southwest";
        if (yaw < 292.5) return "West";
        return "Northwest";
    }

}