package com.ryvione.falsememory.world.structures.templates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StructureTemplate {

    protected ServerLevel level;
    protected BlockPos origin;

    public StructureTemplate(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    protected void setBlock(int x, int y, int z, BlockState state) {
        level.setBlock(origin.offset(x, y, z), state, 3);
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    setBlock(x, y, z, state);
                }
            }
        }
    }

    protected BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }
}
