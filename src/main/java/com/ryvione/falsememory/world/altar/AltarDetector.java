package com.ryvione.falsememory.world.altar;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.Blocks;

public class AltarDetector {

    public static boolean checkAltar(ServerPlayer player, ServerLevel level, PlayerMemory memory) {
        BlockPos center = player.blockPosition();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!isAltarBlock(level, pos, dx, dz, player, memory)) return false;
            }
        }
        return true;
    }

    private static boolean isAltarBlock(ServerLevel level, BlockPos pos,
                                         int dx, int dz, ServerPlayer player,
                                         PlayerMemory memory) {
        if (dx == 0 && dz == 0) {
            var be = level.getBlockEntity(pos);
            if (be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
                var book = lectern.getBook();
                if (book.getItem() instanceof WrittenBookItem) {
                    var customData = book.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    var tag = customData != null ? customData.copyTag() : null;
                    String title = tag != null ? tag.getString("title") : "";
                    return title.equals(player.getName().getString());
                }
            }
            return false;
        }

        String mostCrafted = memory.craftedItems.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse("minecraft:cobblestone");

        if ((dx == -1 && dz == -1) || (dx == 1 && dz == 1)) {
            return level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK);
        }

        if (!memory.deathPositions.isEmpty()) {
            BlockPos death = memory.deathPositions.get(0);
            return level.getBlockState(pos).blocksMotion();
        }

        return level.getBlockState(pos).blocksMotion();
    }
}