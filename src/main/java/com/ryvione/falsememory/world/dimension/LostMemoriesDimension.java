package com.ryvione.falsememory.world.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.ryvione.falsememory.FalseMemory;

public class LostMemoriesDimension {
    public static final ResourceKey<Level> LOST_MEMORIES_KEY =
        ResourceKey.create(Registries.DIMENSION, 
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "lost_memories"));

    public static final String DIMENSION_ID = "falsememory:lost_memories";
}