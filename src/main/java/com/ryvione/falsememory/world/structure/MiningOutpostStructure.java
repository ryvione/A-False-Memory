package com.ryvione.falsememory.world.structure;

import com.ryvione.falsememory.journal.JournalGenerator;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MiningOutpostStructure {

    public static void spawn(ServerLevel level, BlockPos origin, PlayerMemory memory) {
        StructureBuilder b = new StructureBuilder(level, origin);

        for (int i = 0; i < 20; i++) {
            BlockPos tunnelPos = origin.offset(0, -i, 0);
            b.set(0, -i, 0, Blocks.AIR.defaultBlockState());
            b.set(0, -i + 1, 0, Blocks.AIR.defaultBlockState());
            if (i % 8 == 0 && i > 0) {
                level.setBlock(tunnelPos.east(), Blocks.WALL_TORCH.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
            }
        }

        b.set(1, 0, 0, Blocks.OAK_LOG.defaultBlockState());
        b.set(-1, 0, 0, Blocks.OAK_LOG.defaultBlockState());
        b.set(1, 1, 0, Blocks.OAK_PLANKS.defaultBlockState());
        b.set(-1, 1, 0, Blocks.OAK_PLANKS.defaultBlockState());
        b.set(0, 2, 0, Blocks.OAK_PLANKS.defaultBlockState());

        BlockPos cartPos = b.at(2, 0, 0);
        level.setBlock(cartPos, Blocks.RAIL.defaultBlockState(), 3);
        var cart = new net.minecraft.world.entity.vehicle.MinecartChest(level,
            cartPos.getX() + 0.5, cartPos.getY(), cartPos.getZ() + 0.5);
        ItemStack journal = JournalGenerator.generateMiningOutpostJournal(memory);
        cart.setItem(0, journal);
        cart.setItem(1, new ItemStack(Items.TORCH, 8));
        level.addFreshEntity(cart);

        BlockPos signPos = b.at(0, 1, -1);
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        var signBE = level.getBlockEntity(signPos);
        if (signBE instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            sign.getFrontText().setMessage(0, net.minecraft.network.chat.Component.literal("You always"));
            sign.getFrontText().setMessage(1, net.minecraft.network.chat.Component.literal("place torches"));
            sign.getFrontText().setMessage(2, net.minecraft.network.chat.Component.literal("on the right."));
            sign.getFrontText().setMessage(3, net.minecraft.network.chat.Component.literal("How did I know?"));
            sign.setChanged();
        }
    }
}