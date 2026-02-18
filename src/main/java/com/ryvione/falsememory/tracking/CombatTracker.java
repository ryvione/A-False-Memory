package com.ryvione.falsememory.tracking;

import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTracker {

    private static final Map<UUID, Long> combatStartTicks = new HashMap<>();
    private static final Map<UUID, Boolean> wasInCombat = new HashMap<>();

    public static void onPlayerHurt(ServerPlayer player, PlayerMemory memory,
                                     DamageSource source, float amount) {
        UUID uuid = player.getUUID();
        long tick = player.level().getGameTime();

        if (!wasInCombat.getOrDefault(uuid, false)) {
            combatStartTicks.put(uuid, tick);
            wasInCombat.put(uuid, true);
            memory.totalCombatEvents++;
        }

        var mainhand = player.getMainHandItem();
        if (!mainhand.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(mainhand.getItem()).toString();
            memory.weaponUseCounts.merge(itemId, 1, Integer::sum);

            if (mainhand.getItem() instanceof SwordItem) {
                memory.preferredWeaponType = "sword";
            } else if (mainhand.getItem() instanceof AxeItem) {
                memory.preferredWeaponType = "axe";
            } else if (mainhand.getItem() instanceof BowItem || mainhand.getItem() instanceof CrossbowItem) {
                memory.usesRangedWeapons = true;
                memory.preferredWeaponType = "ranged";
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PotionItem) {
                memory.usesPotions = true;
                break;
            }
        }

        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent < 0.3f) {
            memory.combatsFled++;
            memory.averageHealthWhenFleeing = (memory.averageHealthWhenFleeing + player.getHealth()) / 2f;
            wasInCombat.put(uuid, false);
            long start = combatStartTicks.getOrDefault(uuid, tick);
            memory.totalCombatTicks += (tick - start);
        }
    }

    public static void onCombatEnd(ServerPlayer player, PlayerMemory memory, boolean fled) {
        UUID uuid = player.getUUID();
        long tick = player.level().getGameTime();
        long start = combatStartTicks.getOrDefault(uuid, tick);
        memory.totalCombatTicks += (tick - start);

        if (fled) {
            memory.combatsFled++;
        } else {
            memory.combatsStoodGround++;
        }
        wasInCombat.put(uuid, false);
        combatStartTicks.remove(uuid);
    }
}