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

        b.hollow(-2, 0, -2, 2, 3, 2,
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState());

        b.set(0, 3, 0, Blocks.AIR.defaultBlockState());
        b.set(1, 2, 0, Blocks.GLASS_PANE.defaultBlockState());
        b.set(-1, 2, 0, Blocks.GLASS_PANE.defaultBlockState());

        b.set(0, 0, 2, Blocks.AIR.defaultBlockState());
        b.set(0, 1, 2, Blocks.AIR.defaultBlockState());

        b.set(-1, 1, -1, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(1, 1, -1, Blocks.FURNACE.defaultBlockState());

        BlockPos bedPos = b.at(0, 1, 1);
        level.setBlock(bedPos, Blocks.RED_BED.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);

        BlockPos torchPos1 = b.at(-2, 2, 0);
        BlockPos torchPos2 = b.at(2, 2, 0);
        level.setBlock(torchPos1, Blocks.WALL_TORCH.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        level.setBlock(torchPos2, Blocks.WALL_TORCH.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);

        BlockPos chestPos = b.at(0, 1, -1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chestBE = level.getBlockEntity(chestPos);
        if (chestBE instanceof ChestBlockEntity chest) {
            ItemStack journal = JournalGenerator.generateAbandonedCampJournal(memory);
            chest.setItem(0, journal);
            chest.setItem(1, new ItemStack(net.minecraft.world.item.Items.BREAD, 2));
            chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.APPLE, 1));
        }
    }
}