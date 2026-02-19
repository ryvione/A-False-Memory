package com.ryvione.falsememory;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ModConfigSpec SPEC;
    public static final Config INSTANCE;

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
        INSTANCE = pair.getLeft();
        SPEC    = pair.getRight();
    }

    public final ModConfigSpec.BooleanValue enabled;
    public final ModConfigSpec.IntValue gracePeriodDays;
    public final ModConfigSpec.IntValue intensity;
    public final ModConfigSpec.IntValue frequency;

    public final ModConfigSpec.BooleanValue enableChatEvents;
    public final ModConfigSpec.BooleanValue enableBlockEvents;
    public final ModConfigSpec.BooleanValue enableMemoryBook;
    public final ModConfigSpec.BooleanValue enableOnlyOne;
    public final ModConfigSpec.BooleanValue enableFourthWall;
    public final ModConfigSpec.BooleanValue enableSleepHorror;
    public final ModConfigSpec.BooleanValue enableSilentWatcher;
    public final ModConfigSpec.BooleanValue enableEscalatingNotes;

    public final ModConfigSpec.DoubleValue manhuntSpeedMultiplier;

    Config(ModConfigSpec.Builder builder) {
        builder.comment("A False Memory — General Settings").push("general");

        enabled = builder
            .comment("Master toggle. false = mod completely disabled.")
            .define("enabled", true);

        gracePeriodDays = builder
            .comment("In-game days before anything starts. (0-30)")
            .defineInRange("grace_period_days", 3, 0, 30);

        intensity = builder
            .comment("Horror intensity (1-10). 1 = subtle, 5 = intended, 10 = relentless.")
            .defineInRange("intensity", 5, 1, 10);

        frequency = builder
            .comment("Events per night (1-10). 1 = rare, 5 = most nights, 10 = constant.")
            .defineInRange("frequency", 5, 1, 10);

        builder.pop().comment("A False Memory — Feature Toggles").push("features");

        enableChatEvents = builder
            .comment("Fake join messages, echoed chat, false system logs.")
            .define("enable_chat_events", true);

        enableBlockEvents = builder
            .comment("Block shifts, missing torches, mining presence, rooftop footsteps.")
            .define("enable_block_events", true);

        enableMemoryBook = builder
            .comment("Memory book drops, predictive chest, inventory horror.")
            .define("enable_memory_book", true);

        enableOnlyOne = builder
            .comment("Allow The Only One to spawn (final confrontation).")
            .define("enable_only_one", true);

        enableFourthWall = builder
            .comment("Fourth-wall events using real username and system clock.")
            .define("enable_fourth_wall", true);

        enableSleepHorror = builder
            .comment("Subtle room changes every time the player wakes up.")
            .define("enable_sleep_horror", true);

        enableSilentWatcher = builder
            .comment("Entity stands at edge of render distance, stares, then vanishes.")
            .define("enable_silent_watcher", true);

        enableEscalatingNotes = builder
            .comment("Three-stage note sequence that builds across multiple days.")
            .define("enable_escalating_notes", true);

        builder.pop().comment("A False Memory — Combat & Manhunt").push("combat");

        manhuntSpeedMultiplier = builder
            .comment("Speed multiplier for The Only One during manhunt phase. (0.5-2.0)")
            .defineInRange("manhunt_speed_multiplier", 1.0, 0.5, 2.0);

        builder.pop();
    }

    public static boolean isEnabled()             { return INSTANCE.enabled.get(); }
    public static float getIntensityMultiplier()  { return INSTANCE.intensity.get() / 10.0f; }
    public static boolean fourthWallEnabled()     { return INSTANCE.enabled.get() && INSTANCE.enableFourthWall.get(); }
    public static boolean sleepHorrorEnabled()    { return INSTANCE.enabled.get() && INSTANCE.enableSleepHorror.get(); }
    public static boolean silentWatcherEnabled()  { return INSTANCE.enabled.get() && INSTANCE.enableSilentWatcher.get(); }
    public static boolean escalatingNotesEnabled(){ return INSTANCE.enabled.get() && INSTANCE.enableEscalatingNotes.get(); }

    public static boolean shouldEventFire(java.util.Random rng) {
        if (!INSTANCE.enabled.get()) return false;
        return rng.nextFloat() < (INSTANCE.frequency.get() / 10.0f);
    }
}
