package com.ryvione.falsememory.world.structure;

import com.ryvione.falsememory.journal.JournalGenerator;
import com.ryvione.falsememory.journal.books.*;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

public class ArchiveStructure {

    public static void spawn(ServerLevel level, BlockPos origin, PlayerMemory memory) {
        StructureBuilder b = new StructureBuilder(level, origin);

        b.hollow(-6, 0, -6, 6, 6, 6,
            Blocks.STONE_BRICKS.defaultBlockState(),
            Blocks.AIR.defaultBlockState());

        b.fill(-5, 1, -5, 5, 4, -5, Blocks.BOOKSHELF.defaultBlockState());
        b.fill(-5, 1, 5, 5, 4, 5, Blocks.BOOKSHELF.defaultBlockState());
        b.fill(-5, 1, -4, -5, 4, 4, Blocks.BOOKSHELF.defaultBlockState());
        b.fill(5, 1, -4, 5, 4, 4, Blocks.BOOKSHELF.defaultBlockState());

        b.set(0, 1, 0, Blocks.ENCHANTING_TABLE.defaultBlockState());

        placeBook(level, b.at(0, 2, -3), JournalGenerator.generateArchiveMainJournal(memory));
        placeBook(level, b.at(-3, 2, 0), CombatAnalysisBook.generate(memory));
        placeBook(level, b.at(3, 2, 0), PredictionModelBook.generate(memory));
        placeBook(level, b.at(0, 2, 3), LoreBook.generate(memory));
        placeBook(level, b.at(-2, 2, -2), DataDumpBook.generate(memory));
        placeBook(level, b.at(2, 2, 2), SummoningBook.generate(memory));

        for (int i = 0; i < 4; i++) {
            int dx = (i < 2) ? (i == 0 ? -4 : 4) : 0;
            int dz = (i < 2) ? 0 : (i == 2 ? -4 : 4);
            level.setBlock(b.at(dx, 3, dz), Blocks.WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
        }

        BlockPos chestPos = b.at(0, 1, 5);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chestBE = level.getBlockEntity(chestPos);
        if (chestBE instanceof ChestBlockEntity chest) {
            chest.setItem(0, SummoningBook.generate(memory));
        }
    }

    private static void placeBook(ServerLevel level, BlockPos pos, ItemStack book) {
        level.setBlock(pos, Blocks.LECTERN.defaultBlockState(), 3);
        var be = level.getBlockEntity(pos);
        if (be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
            lectern.setBook(book);
            lectern.setChanged();
        }
    }
}