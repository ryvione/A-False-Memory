package com.ryvione.falsememory.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import com.ryvione.falsememory.memory.PlayerMemory;

import java.util.*;

public class TrapAnalyzer {

    public static class TrapMechanism {
        public BlockPos triggerPos;
        public List<BlockPos> affectedBlocks;
        public String trapType;
        public int complexity;

        public TrapMechanism(BlockPos trigger, String type) {
            this.triggerPos = trigger;
            this.trapType = type;
            this.affectedBlocks = new ArrayList<>();
            this.complexity = 0;
        }
    }

    private static final Map<BlockPos, TrapMechanism> detectedTraps = new HashMap<>();

    public static void analyzeTrapAroundPos(Level level, BlockPos center, PlayerMemory memory) {
        int range = 16;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (isPressurePlate(state)) {
                        analyzePressurePlateTrap(level, checkPos, memory);
                    } else if (isPiston(state)) {
                        analyzePistonTrap(level, checkPos, memory);
                    } else if (isRedstoneComponent(state)) {
                        analyzeRedstoneCircuit(level, checkPos, memory);
                    } else if (isDoor(state)) {
                        analyzeDoorMechanism(level, checkPos, memory);
                    }
                }
            }
        }
    }

    private static void analyzePressurePlateTrap(Level level, BlockPos pos, PlayerMemory memory) {
        TrapMechanism trap = new TrapMechanism(pos, "pressure_plate");

        BlockPos above = pos.above();
        BlockPos below = pos.below();

        if (level.getBlockState(below).getMaterial().isSolid()) {
            trap.affectedBlocks.add(below);
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos check = pos.offset(x, 0, z);
                if (isPiston(level.getBlockState(check))) {
                    trap.affectedBlocks.add(check);
                    trap.complexity++;
                }
            }
        }

        memory.detectedTraps.add(trap);
    }

    private static void analyzePistonTrap(Level level, BlockPos pos, PlayerMemory memory) {
        TrapMechanism trap = new TrapMechanism(pos, "piston");

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PistonBaseBlock) {
            trap.affectedBlocks.add(pos.relative(state.getValue(PistonBaseBlock.FACING)));
            trap.complexity = 2;
        }

        memory.detectedTraps.add(trap);
    }

    private static void analyzeRedstoneCircuit(Level level, BlockPos pos, PlayerMemory memory) {
        TrapMechanism trap = new TrapMechanism(pos, "redstone");

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof RedstoneWireBlock) {
            trap.complexity = 3;
            findRedstoneConnections(level, pos, trap, 0);
        }

        memory.detectedTraps.add(trap);
    }

    private static void findRedstoneConnections(Level level, BlockPos pos, TrapMechanism trap, int depth) {
        if (depth > 8 || trap.affectedBlocks.size() > 20) return;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos check = pos.offset(x, y, z);
                    if (!trap.affectedBlocks.contains(check) && isRedstoneComponent(level.getBlockState(check))) {
                        trap.affectedBlocks.add(check);
                        findRedstoneConnections(level, check, trap, depth + 1);
                    }
                }
            }
        }
    }

    private static void analyzeDoorMechanism(Level level, BlockPos pos, PlayerMemory memory) {
        TrapMechanism trap = new TrapMechanism(pos, "door");

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos other = pos.relative(((DoorBlock)state.getBlock()).getHalf(state) == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP);
            trap.affectedBlocks.add(other);
            trap.complexity = 1;
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos check = pos.offset(x, 0, z);
                if (isPressurePlate(level.getBlockState(check))) {
                    trap.affectedBlocks.add(check);
                    trap.complexity++;
                }
            }
        }

        memory.detectedTraps.add(trap);
    }

    private static boolean isPressurePlate(BlockState state) {
        Block block = state.getBlock();
        return block instanceof PressurePlateBlock ||
               block instanceof WeightedPressurePlateBlock;
    }

    private static boolean isPiston(BlockState state) {
        Block block = state.getBlock();
        return block instanceof PistonBaseBlock ||
               block instanceof PistonHeadBlock;
    }

    private static boolean isRedstoneComponent(BlockState state) {
        Block block = state.getBlock();
        return block instanceof RedstoneWireBlock ||
               block instanceof RepeaterBlock ||
               block instanceof ComparatorBlock ||
               block instanceof TargetBlock;
    }

    private static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock;
    }

    public static List<TrapMechanism> getDetectedTraps(PlayerMemory memory) {
        return memory.detectedTraps;
    }

    public static TrapMechanism findTrapAt(PlayerMemory memory, BlockPos pos, int tolerance) {
        for (TrapMechanism trap : memory.detectedTraps) {
            if (trap.triggerPos.closerThan(pos, tolerance)) {
                return trap;
            }
        }
        return null;
    }

    public static int getAverageComplexity(PlayerMemory memory) {
        if (memory.detectedTraps.isEmpty()) return 0;
        return memory.detectedTraps.stream()
            .mapToInt(t -> t.complexity)
            .sum() / memory.detectedTraps.size();
    }
}