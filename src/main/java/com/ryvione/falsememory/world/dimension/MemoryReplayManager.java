package com.ryvione.falsememory.world.dimension;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;


public class MemoryReplayManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServerPlayer player;
    private final PlayerMemory memory;
    private final ServerLevel lostMemoriesLevel;

    private int replayTick = 0;
    private int totalReplayTicks = 0;
    private boolean isReplaying = false;
    private final Queue<BlockPlacementEvent> placementQueue = new LinkedList<>();
    private final Queue<MovementSnapshot> movementSnapshots = new LinkedList<>();

    private static final int REPLAY_SPEED = 2; 
    private static final int MEMORY_FADE_DURATION = 600; 

    public MemoryReplayManager(ServerPlayer player, PlayerMemory memory, ServerLevel lostMemoriesLevel) {
        this.player = player;
        this.memory = memory;
        this.lostMemoriesLevel = lostMemoriesLevel;
        this.reconstructReplay();
    }


    private void reconstructReplay() {
        LOGGER.info("[FalseMemory] Reconstructing memories for {}", player.getName().getString());

        BlockPos firstBase = memory.firstLoginPos != null ? 
            memory.firstLoginPos : new BlockPos(0, 64, 0);

        reconstructBaseStructure(firstBase);

        reconstructMovementPatterns();

        totalReplayTicks = placementQueue.size() * REPLAY_SPEED;
        isReplaying = true;

        LOGGER.info("[FalseMemory] Replay reconstructed with {} block placements", placementQueue.size());
    }


    private void reconstructBaseStructure(BlockPos centerPos) {

        for (int x = -25; x < 25; x++) {
            for (int z = -25; z < 25; z++) {
                BlockPos pos = centerPos.offset(x, 0, z);
                lostMemoriesLevel.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }

        memory.blockPlacementCounts.forEach((blockId, count) -> {
            try {
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getOptional(net.minecraft.resources.ResourceLocation.parse(blockId));
                
                if (block.isPresent() && count > 0) {
                    int numToPlace = Math.min(count / 10, 20);
                    for (int i = 0; i < numToPlace; i++) {
                        int rx = centerPos.getX() + (int)(Math.random() * 30 - 15);
                        int ry = centerPos.getY() + 1;
                        int rz = centerPos.getZ() + (int)(Math.random() * 30 - 15);
                        BlockPos randomPos = new BlockPos(rx, ry, rz);
                        
                        if (lostMemoriesLevel.getBlockState(randomPos.below()).getMaterial().isSolid()) {
                            lostMemoriesLevel.setBlock(randomPos, block.get().defaultBlockState(), 3);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        buildSimpleShelter(centerPos);
    }


    private void buildSimpleShelter(BlockPos pos) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                lostMemoriesLevel.setBlock(pos.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                lostMemoriesLevel.setBlock(pos.offset(x, 1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                lostMemoriesLevel.setBlock(pos.offset(x, 2, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                lostMemoriesLevel.setBlock(pos.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 3);
                lostMemoriesLevel.setBlock(pos.offset(x, 2, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        lostMemoriesLevel.setBlock(pos.offset(0, 1, -3), Blocks.OAK_DOOR.defaultBlockState(), 3);
    }


    private void reconstructMovementPatterns() {
        if (memory.loginPositions.isEmpty()) return;

        BlockPos firstLogin = memory.firstLoginPos != null ? 
            memory.firstLoginPos : memory.loginPositions.get(0);

        for (int i = 0; i < Math.min(memory.loginPositions.size(), 10); i++) {
            BlockPos pos = memory.loginPositions.get(i);
            movementSnapshots.add(new MovementSnapshot(
                new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5),
                memory.worldDayCount > 0 ? i * (memory.worldDayCount / 10) : i
            ));
        }
    }


    public void tick() {
        if (!isReplaying) return;

        replayTick++;

        if (replayTick % REPLAY_SPEED == 0 && !placementQueue.isEmpty()) {
            BlockPlacementEvent event = placementQueue.poll();
            placeBlockAtPosition(event.position, event.blockState);
        }

        if (placementQueue.isEmpty() && replayTick > MEMORY_FADE_DURATION) {
            finishReplay();
        }
    }

    private void placeBlockAtPosition(BlockPos pos, String blockId) {
        try {
            var blockOpt = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getOptional(net.minecraft.resources.ResourceLocation.parse(blockId));
            
            blockOpt.ifPresent(block -> {
                lostMemoriesLevel.setBlock(pos, block.defaultBlockState(), 3);
            });
        } catch (Exception ignored) {}
    }


    private void finishReplay() {
        isReplaying = false;
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§8It's been watching... since the very beginning."));

        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (memory.inferredHomePos != null) {
                    player.teleportTo(
                        (net.minecraft.server.level.ServerLevel) memory.inferredHomePos.getX() + 0.5,
                        memory.inferredHomePos.getY() + 1,
                        memory.inferredHomePos.getZ() + 0.5
                    );
                }
            }
        }, 5000); 
    }

    public boolean isReplaying() { return isReplaying; }
    public int getReplayProgress() { return (int)((float)replayTick / totalReplayTicks * 100); }


    private static class BlockPlacementEvent {
        BlockPos position;
        String blockState;

        BlockPlacementEvent(BlockPos position, String blockState) {
            this.position = position;
            this.blockState = blockState;
        }
    }

    public static class MovementSnapshot {
        public final Vec3 position;
        public final long day;

        MovementSnapshot(Vec3 position, long day) {
            this.position = position;
            this.day = day;
        }
    }
}