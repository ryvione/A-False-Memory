package com.ryvione.falsememory;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.entity.ModEntities;
import com.ryvione.falsememory.network.ModNetwork;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(FalseMemory.MOD_ID)
public class FalseMemory {

    public static final String MOD_ID = "falsememory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FalseMemory(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[FalseMemory] Initializing...");

        ModEntities.register(modEventBus);
        ModNetwork.register(modEventBus);

        modEventBus.addListener(this::registerEntityAttributes);

        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.register(com.ryvione.falsememory.debug.DebugCommands.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[FalseMemory] Ready.");
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        
        event.put(ModEntities.THE_OBSESSED.get(),
            com.ryvione.falsememory.entity.TheObsessedEntity.createAttributes().build());
        event.put(ModEntities.THE_WITNESS.get(),
            com.ryvione.falsememory.entity.TheWitnessEntity.createAttributes().build());
        event.put(ModEntities.THE_ONLY_ONE.get(),
            com.ryvione.falsememory.entity.TheOnlyOneEntity.createAttributes().build());
    }
}