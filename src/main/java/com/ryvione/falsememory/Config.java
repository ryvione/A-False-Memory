package com.ryvione.falsememory;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ModConfigSpec SPEC;
    public static final Config INSTANCE;

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.BooleanValue enabled;
    public final ModConfigSpec.IntValue gracePeriodDays;
    public final ModConfigSpec.IntValue intensity;
    public final ModConfigSpec.IntValue frequency;
    public final ModConfigSpec.BooleanValue enableChatEvents;
    public final ModConfigSpec.BooleanValue enableBlockEvents;
    public final ModConfigSpec.BooleanValue enableMemoryBook;
    public final ModConfigSpec.BooleanValue enableOnlyOne;
    public final ModConfigSpec.DoubleValue manhuntSpeedMultiplier;

    Config(ModConfigSpec.Builder builder) {
        builder.comment("A False Memory — Configuration").push("general");

        enabled = builder
            .comment("Enable or disable the mod entirely")
            .define("enabled", true);

        gracePeriodDays = builder
            .comment("Days before anything starts")
            .defineInRange("grace_period_days", 3, 0, 30);

        intensity = builder
            .comment("Horror intensity (1=subtle, 10=overwhelming)")
            .defineInRange("intensity", 5, 1, 10);

        frequency = builder
            .comment("Event frequency per night (1=rare, 10=constant)")
            .defineInRange("frequency", 5, 1, 10);

        builder.pop().push("features");

        enableChatEvents = builder
            .define("enable_chat_events", true);

        enableBlockEvents = builder
            .define("enable_block_events", true);

        enableMemoryBook = builder
            .define("enable_memory_book", true);

        enableOnlyOne = builder
            .comment("Enable the final boss")
            .define("enable_only_one", true);

        manhuntSpeedMultiplier = builder
            .comment("Manhunt phase speed scaling")
            .defineInRange("manhunt_speed_multiplier", 1.0, 0.5, 2.0);

        builder.pop();
    }

    public static float getIntensityMultiplier() {
        return INSTANCE.intensity.get() / 10.0f;
    }

    public static boolean shouldEventFire(java.util.Random rng) {
        if (!INSTANCE.enabled.get()) return false;
        float chance = INSTANCE.frequency.get() / 10.0f;
        return rng.nextFloat() < chance;
    }
}