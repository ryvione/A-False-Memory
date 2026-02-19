package com.ryvione.falsememory;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClothConfigIntegration {

    public static boolean isAvailable() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Screen buildConfigScreen(Screen parent) {
        var builder = me.shedaniel.clothconfig2.api.ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(net.minecraft.network.chat.Component.literal("A False Memory — Config"))
            .setSavingRunnable(() -> Config.SPEC.save());

        var entryBuilder = builder.entryBuilder();

        var general = builder.getOrCreateCategory(net.minecraft.network.chat.Component.literal("General"));

        general.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Enabled"), Config.INSTANCE.enabled.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Master toggle for the entire mod."))
            .setSaveConsumer(v -> Config.INSTANCE.enabled.set(v))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(net.minecraft.network.chat.Component.literal("Grace Period (days)"), Config.INSTANCE.gracePeriodDays.get(), 0, 30)
            .setDefaultValue(3)
            .setTooltip(net.minecraft.network.chat.Component.literal("In-game days before anything starts."))
            .setSaveConsumer(v -> Config.INSTANCE.gracePeriodDays.set(v))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(net.minecraft.network.chat.Component.literal("Intensity"), Config.INSTANCE.intensity.get(), 1, 10)
            .setDefaultValue(5)
            .setTooltip(net.minecraft.network.chat.Component.literal("1 = subtle  |  5 = intended  |  10 = relentless"))
            .setSaveConsumer(v -> Config.INSTANCE.intensity.set(v))
            .build());

        general.addEntry(entryBuilder
            .startIntSlider(net.minecraft.network.chat.Component.literal("Event Frequency"), Config.INSTANCE.frequency.get(), 1, 10)
            .setDefaultValue(5)
            .setTooltip(net.minecraft.network.chat.Component.literal("Events per night. 1 = rare  |  10 = constant."))
            .setSaveConsumer(v -> Config.INSTANCE.frequency.set(v))
            .build());

        var features = builder.getOrCreateCategory(net.minecraft.network.chat.Component.literal("Features"));

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Chat Events"), Config.INSTANCE.enableChatEvents.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Fake join messages, echoed chat, false system logs."))
            .setSaveConsumer(v -> Config.INSTANCE.enableChatEvents.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Block Events"), Config.INSTANCE.enableBlockEvents.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Block shifts, missing torches, mining presence, rooftop footsteps."))
            .setSaveConsumer(v -> Config.INSTANCE.enableBlockEvents.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Memory Books"), Config.INSTANCE.enableMemoryBook.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Memory book drops, predictive chest, inventory horror."))
            .setSaveConsumer(v -> Config.INSTANCE.enableMemoryBook.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("The Only One"), Config.INSTANCE.enableOnlyOne.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Allow The Only One to spawn and initiate the final confrontation."))
            .setSaveConsumer(v -> Config.INSTANCE.enableOnlyOne.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Fourth-Wall Breaks"), Config.INSTANCE.enableFourthWall.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Uses your real username and system clock."))
            .setSaveConsumer(v -> Config.INSTANCE.enableFourthWall.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Sleep Horror"), Config.INSTANCE.enableSleepHorror.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Subtle room changes every time you wake up."))
            .setSaveConsumer(v -> Config.INSTANCE.enableSleepHorror.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Silent Watcher"), Config.INSTANCE.enableSilentWatcher.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Entity stands at edge of render distance, stares, then vanishes."))
            .setSaveConsumer(v -> Config.INSTANCE.enableSilentWatcher.set(v))
            .build());

        features.addEntry(entryBuilder
            .startBooleanToggle(net.minecraft.network.chat.Component.literal("Escalating Notes"), Config.INSTANCE.enableEscalatingNotes.get())
            .setDefaultValue(true)
            .setTooltip(net.minecraft.network.chat.Component.literal("Three-stage note sequence across multiple days."))
            .setSaveConsumer(v -> Config.INSTANCE.enableEscalatingNotes.set(v))
            .build());

        var combat = builder.getOrCreateCategory(net.minecraft.network.chat.Component.literal("Combat & Manhunt"));

        combat.addEntry(entryBuilder
            .startDoubleField(net.minecraft.network.chat.Component.literal("Manhunt Speed Multiplier"), Config.INSTANCE.manhuntSpeedMultiplier.get())
            .setDefaultValue(1.0)
            .setMin(0.5).setMax(2.0)
            .setTooltip(net.minecraft.network.chat.Component.literal("Speed multiplier for The Only One during manhunt phase."))
            .setSaveConsumer(v -> Config.INSTANCE.manhuntSpeedMultiplier.set(v))
            .build());

        return builder.build();
    }
}
