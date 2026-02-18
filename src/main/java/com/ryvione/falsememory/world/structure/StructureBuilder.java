package com.ryvione.falsememory.world.structure;

import com.ryvione.falsememory.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class StructureBuilder {

    private final ServerLevel level;
    private final BlockPos origin;

    public StructureBuilder(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    public StructureBuilder set(int dx, int dy, int dz, BlockState state) {
        level.setBlock(origin.offset(dx, dy, dz), state, 3);
        return this;
    }

    public StructureBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    level.setBlock(origin.offset(x, y, z), state, 3);
        return this;
    }

    public StructureBuilder hollow(int x1, int y1, int z1, int x2, int y2, int z2,
                                    BlockState wall, BlockState interior) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    boolean edge = x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2;
                    level.setBlock(origin.offset(x, y, z), edge ? wall : interior, 3);
                }
        return this;
    }

    public StructureBuilder clearSpace(int x1, int y1, int z1, int x2, int y2, int z2) {
        return fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    public BlockPos at(int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }

    public ServerLevel getLevel() { return level; }
    public BlockPos getOrigin() { return origin; }
}