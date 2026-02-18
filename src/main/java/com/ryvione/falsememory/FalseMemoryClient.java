package com.ryvione.falsememory;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.client.MainMenuSplash;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = FalseMemory.MOD_ID, dist = Dist.CLIENT)
public class FalseMemoryClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FalseMemoryClient(IEventBus modEventBus) {
        LOGGER.info("[FalseMemory] Client initializing.");
        NeoForge.EVENT_BUS.register(MainMenuSplash.class);
    }
}