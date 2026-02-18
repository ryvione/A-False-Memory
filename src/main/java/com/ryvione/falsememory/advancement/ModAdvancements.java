package com.ryvione.falsememory.advancement;

import com.ryvione.falsememory.FalseMemory;
import net.minecraft.resources.ResourceLocation;

public class ModAdvancements {

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, path);
    }
}