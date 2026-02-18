package com.ryvione.falsememory.world.scar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

public class ScarPlacer {

    private static final Random RNG = new Random();

    public static void placeScar(ServerLevel level, BlockPos deathPos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 6) continue;
                BlockPos surface = deathPos.offset(dx, 0, dz);
                BlockState state = level.getBlockState(surface);

                if (state.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(surface, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)) {
                    if (RNG.nextInt(3) == 0)
                        level.setBlock(surface, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
                } else if (state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)) {
                    level.setBlock(surface, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                }
            }
        }

        placeGraveMarker(level, deathPos);
    }

    private static void placeGraveMarker(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir()) {
            level.setBlock(above, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
            BlockPos signPos = above.above();
            level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            var be = level.getBlockEntity(signPos);
            if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                sign.getFrontText().setMessage(0, net.minecraft.network.chat.Component.literal("You were here."));
                sign.getFrontText().setMessage(1, net.minecraft.network.chat.Component.literal(""));
                sign.getFrontText().setMessage(2, net.minecraft.network.chat.Component.literal("I remember."));
                sign.setChanged();
            }
        }
    }
}