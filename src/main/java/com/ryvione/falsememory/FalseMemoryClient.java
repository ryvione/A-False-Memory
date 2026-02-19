package com.ryvione.falsememory;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.client.MainMenuSplash;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = FalseMemory.MOD_ID, dist = Dist.CLIENT)
public class FalseMemoryClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FalseMemoryClient(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[FalseMemory] Client initializing.");

        NeoForge.EVENT_BUS.register(MainMenuSplash.class);

        if (ClothConfigIntegration.isAvailable()) {
            LOGGER.info("[FalseMemory] Cloth Config detected — registering config screen.");
            modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> ClothConfigIntegration.buildConfigScreen(parent)
            );
        } else {
            LOGGER.info("[FalseMemory] Cloth Config not found — using falsememory-common.toml.");
        }
    }
}
