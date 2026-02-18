package com.ryvione.falsememory.events;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.entity.TheObsessedEntity;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
    private static final Random RNG = new Random();

    public static void subtleBlockShift(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("block_shift")) return;

        BlockPos searchCenter = memory.inferredHomePos != null
            ? memory.inferredHomePos : player.blockPosition();

        for (int attempts = 0; attempts < 30; attempts++) {
            BlockPos pos = searchCenter.offset(
                RNG.nextInt(11) - 5, RNG.nextInt(5) - 2, RNG.nextInt(11) - 5);
            BlockState state = level.getBlockState(pos);

            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK
                && level.getBlockState(pos.above()).isAir()) {

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
        playSoundForPlayer(player, "minecraft:ambient.cave", SoundSource.AMBIENT,
            0.3f, 0.5f + RNG.nextFloat() * 0.3f);
        memory.markTriggered("whisper");
    }

    public static void missingTorch(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("missing_torch")) return;

        BlockPos searchCenter = memory.inferredHomePos != null
            ? memory.inferredHomePos : player.blockPosition();

        for (int attempts = 0; attempts < 50; attempts++) {
            BlockPos pos = searchCenter.offset(
                RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
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

        BlockPos searchCenter = memory.inferredHomePos != null
            ? memory.inferredHomePos : player.blockPosition();

        for (int attempts = 0; attempts < 60; attempts++) {
            BlockPos pos = searchCenter.offset(
                RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
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

        BlockPos searchCenter = memory.inferredHomePos != null
            ? memory.inferredHomePos : player.blockPosition();

        String[] lines;
        if (!memory.chatHistory.isEmpty()) {
            String pastChat = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
            lines = new String[]{
                "I remember", "you said:",
                pastChat.substring(0, Math.min(15, pastChat.length())), ""
            };
        } else {
            lines = new String[]{"I see you.", "", "Do you see", "me?"};
        }

        for (int attempts = 0; attempts < 80; attempts++) {
            BlockPos pos = searchCenter.offset(
                RNG.nextInt(21) - 10, RNG.nextInt(9) - 4, RNG.nextInt(21) - 10);
            BlockState state = level.getBlockState(pos);

            if (state.is(Blocks.OAK_SIGN) || state.is(Blocks.OAK_WALL_SIGN)
                || state.is(Blocks.BIRCH_SIGN) || state.is(Blocks.SPRUCE_SIGN)) {

                var blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
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

    public static void fakePlayerJoinMessage(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_join")) return;
        String playerName = player.getName().getString();
        Component msg = Component.translatable("multiplayer.player.joined",
            Component.literal(playerName).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(msg);
        memory.markTriggered("fake_join");
    }

    public static void memoryBookAppears(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("memory_book")) return;

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag bookTag = new CompoundTag();
        bookTag.putString("title", "Observations");
        bookTag.putString("author", "???");

        ListTag pages = new ListTag();

        pages.add(StringTag.valueOf(buildBookPage(
            "You always face " + getDirectionName(memory.getDominantFacingYaw()) + ".\n\n" +
            "You have died " + memory.totalDeaths + " time" + (memory.totalDeaths == 1 ? "" : "s") + ".\n\n" +
            "I remember each one."
        )));

        if (memory.inferredHomePos != null) {
            BlockPos home = memory.inferredHomePos;
            pages.add(StringTag.valueOf(buildBookPage(
                "You always come back to the same place.\n\nNear " +
                home.getX() + ", " + home.getZ() + ".\n\nI'll be there.\nWaiting."
            )));
        }

        if (!memory.visitedBiomes.isEmpty()) {
            String biome = memory.visitedBiomes.iterator().next()
                .replace("minecraft:", "").replace("_", " ");
            pages.add(StringTag.valueOf(buildBookPage(
                "I followed you through the " + biome + ".\n\nYou didn't notice.\n\nYou never notice."
            )));
        }

        if (!memory.weaponUseCounts.isEmpty()) {
            pages.add(StringTag.valueOf(buildBookPage(
                "You fight with a " + memory.preferredWeaponType + ".\n\n" +
                "You flee when you have " + String.format("%.1f", memory.averageHealthWhenFleeing) + " hearts left.\n\n" +
                "I know when you'll run."
            )));
        }

        if (memory.totalDeaths > 0 && memory.getMostFrequentDeathCause() != null) {
            pages.add(StringTag.valueOf(buildBookPage(
                "You have died to " + memory.getMostFrequentDeathCause().replace("_", " ") +
                " more than anything else.\n\nI remember the sound you made."
            )));
        }

        bookTag.put("pages", pages);
        book.setTag(bookTag);

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
        com.ryvione.falsememory.advancement.AdvancementTriggers.MEMORY_BOOK);
        memory.markTriggered("memory_book");
    }

    public static void yourOwnDeathSoundDistant(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("death_sound_distant")) return;
        playSoundForPlayer(player, "minecraft:entity.player.death", SoundSource.PLAYERS,
            0.5f, 0.8f + RNG.nextFloat() * 0.4f);
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

        boolean obsessedExists = !level.getEntities(
            ModEntities.THE_OBSESSED.get(),
            entity -> entity instanceof TheObsessedEntity obs &&
                player.getUUID().toString().equals(obs.getTargetPlayerUUID())
        ).isEmpty();

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

        boolean obsessedExists = !level.getEntities(
            ModEntities.THE_OBSESSED.get(),
            entity -> entity instanceof TheObsessedEntity obs &&
                player.getUUID().toString().equals(obs.getTargetPlayerUUID())
        ).isEmpty();

        if (!obsessedExists) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = 60 + RNG.nextInt(20);
            double spawnX = player.getX() + Math.sin(angle) * dist;
            double spawnZ = player.getZ() + Math.cos(angle) * dist;

            BlockPos spawnPos = new BlockPos((int) spawnX, (int) player.getY(), (int) spawnZ);
            while (level.getBlockState(spawnPos).isAir() && spawnPos.getY() > 0) {
                spawnPos = spawnPos.below();
            }
            spawnPos = spawnPos.above();

            TheObsessedEntity obsessed = ModEntities.THE_OBSESSED.get().create(level);
            if (obsessed != null) {
                obsessed.setTargetPlayer(player.getUUID());
                obsessed.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(obsessed);
                LOGGER.info("[FalseMemory] Spawned The Obsessed for {}", player.getName().getString());
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

        List<BlockPos> blocksToMirror = memory.recentlyPlacedBlocks.subList(
            Math.max(0, memory.recentlyPlacedBlocks.size() - 20),
            memory.recentlyPlacedBlocks.size()
        );

        int avgX = 0, avgY = 0, avgZ = 0;
        for (BlockPos b : blocksToMirror) { avgX += b.getX(); avgY += b.getY(); avgZ += b.getZ(); }
        avgX /= blocksToMirror.size(); avgY /= blocksToMirror.size(); avgZ /= blocksToMirror.size();
        final int fAvgX = avgX, fAvgY = avgY, fAvgZ = avgZ;

        for (BlockPos original : blocksToMirror) {
            BlockState state = level.getBlockState(original);
            if (!state.isAir()) {
                BlockPos relPos = new BlockPos(
                    replicaOrigin.getX() + (original.getX() - fAvgX),
                    replicaOrigin.getY() + (original.getY() - fAvgY),
                    replicaOrigin.getZ() + (original.getZ() - fAvgZ)
                );
                level.setBlock(relPos, state, 3);
            }
        }
        memory.markTriggered("replica_base");
    }

    public static void fakeChatFromPast(ServerPlayer player, PlayerMemory memory) {
        if (memory.wasTriggeredToday("fake_chat")) return;
        if (memory.chatHistory.isEmpty()) return;

        String pastMessage = memory.chatHistory.get(RNG.nextInt(memory.chatHistory.size()));
        Component msg = Component.literal("??? ")
            .withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(pastMessage).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(msg);
        memory.markTriggered("fake_chat");
    }

    public static void standingWhereYouStand(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("standing_where_you_stand")) return;
        if (memory.loginPositions.size() < 2) return;

        BlockPos lastLogin = memory.loginPositions.get(memory.loginPositions.size() - 2);
        double dist = player.blockPosition().distSqr(lastLogin);
        if (dist < 400 || dist > 40000) return;

        level.levelEvent(2003, lastLogin, 0);
        playSoundForPlayer(player, "minecraft:entity.enderman.teleport", SoundSource.HOSTILE, 0.2f, 0.7f);
        sendTitle(player, "", "...", 5, 30, 15);
        memory.markTriggered("standing_where_you_stand");
    }

    public static void predictivePlacement(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        if (memory.wasTriggeredToday("predictive_placement")) return;

        String hotbarItem = memory.lastHotbarSnapshot.isEmpty()
            ? null : memory.lastHotbarSnapshot.get(0);

        if (hotbarItem != null) {
            var itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(hotbarItem));
            itemOpt.ifPresent(item -> {
                ItemStack stack = new ItemStack(item);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            });
        }

        sendTitle(player, "", "I already know.", 5, 40, 20);
        playSoundForPlayer(player, "minecraft:entity.enderman.stare", SoundSource.HOSTILE, 0.2f, 0.4f);
        memory.markTriggered("predictive_placement");
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

    static void playSoundForPlayer(ServerPlayer player, String soundId, SoundSource source,
                                    float volume, float pitch) {
        try {
            var soundEvent = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
            if (soundEvent != null) {
                player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
                    source,
                    player.getX(), player.getY(), player.getZ(),
                    volume, pitch,
                    RNG.nextLong()
                ));
            }
        } catch (Exception e) {
            LOGGER.warn("[FalseMemory] Sound failed {}: {}", soundId, e.getMessage());
        }
    }

    static void sendTitle(ServerPlayer player, String title, String subtitle,
                           int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal(subtitle).withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static String getDirectionName(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 22.5 || yaw >= 337.5) return "North";
        if (yaw < 67.5) return "Northeast";
        if (yaw < 112.5) return "East";
        if (yaw < 157.5) return "Southeast";
        if (yaw < 202.5) return "South";
        if (yaw < 247.5) return "Southwest";
        if (yaw < 292.5) return "West";
        return "Northwest";
    }

    private static String buildBookPage(String text) {
        return net.minecraft.network.chat.Component.Serializer.toJson(
            Component.literal(text),
            net.minecraft.core.RegistryAccess.EMPTY
        );
    }
}