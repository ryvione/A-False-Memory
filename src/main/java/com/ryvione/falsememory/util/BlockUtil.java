package com.ryvione.falsememory.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {

    public static BlockPos findSolidGround(ServerLevel level, BlockPos start, int maxDown) {
        BlockPos pos = start;
        for (int i = 0; i < maxDown; i++) {
            if (!level.getBlockState(pos).isAir()) {
                return pos.above();
            }
            pos = pos.below();
        }
        return start;
    }

    public static boolean isSafeToPlace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
            && level.getBlockState(pos.below()).isSolid();
    }

    public static void setIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, state, 3);
        }
    }

    public static void setForced(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }

    public static void clearColumn(ServerLevel level, BlockPos base, int height) {
        for (int i = 0; i < height; i++) {
            level.setBlock(base.above(i), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}