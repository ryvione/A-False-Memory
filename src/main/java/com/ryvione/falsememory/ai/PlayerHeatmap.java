package com.ryvione.falsememory.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class PlayerHeatmap {

    private static final int GRID_SIZE = 16;
    private final Map<Long, Integer> heatmapData = new LinkedHashMap<>();
    private BlockPos hotSpot = null;
    private int maxHeat = 0;

    public void recordVisit(BlockPos pos) {
        long gridKey = getGridKey(pos);
        heatmapData.put(gridKey, heatmapData.getOrDefault(gridKey, 0) + 1);
        
        int heat = heatmapData.get(gridKey);
        if (heat > maxHeat) {
            maxHeat = heat;
            hotSpot = pos;
        }
    }

    private long getGridKey(BlockPos pos) {
        int gridX = pos.getX() / GRID_SIZE;
        int gridZ = pos.getZ() / GRID_SIZE;
        return ((long)gridX << 32) | (gridZ & 0xFFFFFFFFL);
    }

    public BlockPos getHotSpot() {
        if (hotSpot == null) {
            for (Map.Entry<Long, Integer> e : heatmapData.entrySet()) {
                if (e.getValue() == maxHeat) {
                    long key = e.getKey();
                    int gridX = (int)(key >> 32);
                    int gridZ = (int)key;
                    hotSpot = new BlockPos(gridX * GRID_SIZE, 64, gridZ * GRID_SIZE);
                    break;
                }
            }
        }
        return hotSpot;
    }

    public List<BlockPos> getTopLocations(int count) {
        List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(heatmapData.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, sorted.size()); i++) {
            long key = sorted.get(i).getKey();
            int gridX = (int)(key >> 32);
            int gridZ = (int)key;
            result.add(new BlockPos(gridX * GRID_SIZE, 64, gridZ * GRID_SIZE));
        }
        return result;
    }

    public int getHeatAt(BlockPos pos) {
        return heatmapData.getOrDefault(getGridKey(pos), 0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag heatList = new ListTag();
        
        for (Map.Entry<Long, Integer> e : heatmapData.entrySet()) {
            CompoundTag heatTag = new CompoundTag();
            heatTag.putLong("gridKey", e.getKey());
            heatTag.putInt("heat", e.getValue());
            heatList.add(heatTag);
        }
        
        tag.put("heatmapData", heatList);
        if (hotSpot != null) {
            CompoundTag spotTag = new CompoundTag();
            spotTag.putInt("x", hotSpot.getX());
            spotTag.putInt("y", hotSpot.getY());
            spotTag.putInt("z", hotSpot.getZ());
            tag.put("hotSpot", spotTag);
        }
        tag.putInt("maxHeat", maxHeat);
        
        return tag;
    }

    public void load(CompoundTag tag) {
        heatmapData.clear();
        ListTag heatList = tag.getList("heatmapData", Tag.TAG_COMPOUND);
        
        for (int i = 0; i < heatList.size(); i++) {
            CompoundTag heatTag = heatList.getCompound(i);
            heatmapData.put(heatTag.getLong("gridKey"), heatTag.getInt("heat"));
        }
        
        if (tag.contains("hotSpot")) {
            CompoundTag spotTag = tag.getCompound("hotSpot");
            hotSpot = new BlockPos(spotTag.getInt("x"), spotTag.getInt("y"), spotTag.getInt("z"));
        }
        maxHeat = tag.getInt("maxHeat");
    }
}
