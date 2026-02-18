package com.ryvione.falsememory.entity;

import com.ryvione.falsememory.FalseMemory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, FalseMemory.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<TheObsessedEntity>> THE_OBSESSED =
        ENTITY_TYPES.register("the_obsessed", () ->
            EntityType.Builder.<TheObsessedEntity>of(TheObsessedEntity::new, MobCategory.MONSTER)
                .sized(0.6f, 1.8f)
                .clientTrackingRange(80)
                .updateInterval(1)
                .build(ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "the_obsessed").toString())
        );

    public static final DeferredHolder<EntityType<?>, EntityType<TheWitnessEntity>> THE_WITNESS =
        ENTITY_TYPES.register("the_witness", () ->
            EntityType.Builder.<TheWitnessEntity>of(TheWitnessEntity::new, MobCategory.MISC)
                .sized(0.6f, 1.8f)
                .clientTrackingRange(64)
                .updateInterval(10)
                .build(ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "the_witness").toString())
        );

    public static final DeferredHolder<EntityType<?>, EntityType<TheOnlyOneEntity>> THE_ONLY_ONE =
        ENTITY_TYPES.register("the_only_one", () ->
            EntityType.Builder.<TheOnlyOneEntity>of(TheOnlyOneEntity::new, MobCategory.MONSTER)
                .sized(0.6f, 1.8f)
                .clientTrackingRange(128)
                .updateInterval(1)
                .build(ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "the_only_one").toString())
        );

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}