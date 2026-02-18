package com.ryvione.falsememory.advancement;
import com.ryvione.falsememory.FalseMemory;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class AdvancementTriggers {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String FIRST_SIGHTING = "first_sighting";
    public static final String MEMORY_BOOK = "memory_book";
    public static final String RITUAL_COMPLETE = "ritual_complete";
    public static final String ONLY_ONE_SUMMONED = "only_one_summoned";
    public static final String ONLY_ONE_DEFEATED = "only_one_defeated";
    public static final String SURVIVED_MANHUNT = "survived_manhunt";
    public static final String DRAW = "draw";
    public static final String TIER_3 = "tier_3";
    public static final String ARCHIVE_FOUND = "archive_found";
    public static final String DIED_TO_ONLY_ONE = "died_to_only_one";

    public static void grant(ServerPlayer player, String advancementId) {
        var server = player.getServer();
        if (server == null) return;

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            FalseMemory.MOD_ID, advancementId);
        AdvancementHolder holder = server.getAdvancements().get(id);

        if (holder == null) {
            LOGGER.debug("[FalseMemory] Advancement not found: {}", id);
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(holder);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
            LOGGER.debug("[FalseMemory] Granted advancement: {}", advancementId);
        }
    }
}