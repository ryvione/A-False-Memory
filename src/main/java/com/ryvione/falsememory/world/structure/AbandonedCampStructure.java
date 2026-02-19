package com.ryvione.falsememory.world.structure;

import com.ryvione.falsememory.journal.JournalGenerator;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;

public class AbandonedCampStructure {

    public static void spawn(ServerLevel level, BlockPos origin, PlayerMemory memory) {
        StructureBuilder b = new StructureBuilder(level, origin);

        b.fill(-3, 0, -3, 3, 0, 3, Blocks.DIRT.defaultBlockState());
        b.fill(-2, 0, -2, 2, 0, 2, Blocks.GRASS_BLOCK.defaultBlockState());

        b.set(0, 0, 0, Blocks.CAMPFIRE.defaultBlockState());

        b.set(-2, 0, 0, Blocks.OAK_LOG.defaultBlockState());
        b.set(-1, 0, 0, Blocks.OAK_WOOD.defaultBlockState());
        b.set(2, 0, 0, Blocks.OAK_LOG.defaultBlockState());
        b.set(1, 0, 0, Blocks.OAK_WOOD.defaultBlockState());

        b.set(-2, 1, 0, Blocks.DRIED_KELP_BLOCK.defaultBlockState());
        b.set(2, 1, 0, Blocks.DRIED_KELP_BLOCK.defaultBlockState());

        b.set(0, 1, -2, Blocks.SPRUCE_LOG.defaultBlockState());
        b.set(0, 2, -2, Blocks.SPRUCE_LOG.defaultBlockState());

        b.set(-1, 1, -2, Blocks.WHITE_WOOL.defaultBlockState());
        b.set(1, 1, -2, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());

        BlockPos chestPos = b.at(0, 0, -2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chestBE = level.getBlockEntity(chestPos);
        if (chestBE instanceof ChestBlockEntity chest) {
            ItemStack journal = JournalGenerator.generateAbandonedCampJournal(memory);
            chest.setItem(0, journal);
            chest.setItem(1, new ItemStack(net.minecraft.world.item.Items.BONE, 3));
            chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 2));
        }

        b.set(2, 0, -2, Blocks.LANTERN.defaultBlockState());
        b.set(-2, 0, 2, Blocks.LANTERN.defaultBlockState());

        b.set(-1, 0, 2, Blocks.DARK_OAK_LOG.defaultBlockState());
        b.set(1, 0, 2, Blocks.DARK_OAK_LOG.defaultBlockState());
    }
}