package com.ryvione.falsememory.ai;

import net.minecraft.nbt.CompoundTag;

public class AggressivityLevel {

    public enum Level {
        DORMANT(0, "Dormant", 0.0f),
        AWARE(1, "Aware", 0.3f),
        ALERT(2, "Alert", 0.6f),
        HOSTILE(3, "Hostile", 1.0f);

        public final int value;
        public final String name;
        public final float intensity;

        Level(int value, String name, float intensity) {
            this.value = value;
            this.name = name;
            this.intensity = intensity;
        }
    }

    private Level currentLevel = Level.DORMANT;
    private int aggression = 0;
    private int trapComplexity = 0;
    private float aiIntelligence = 0.0f;

    public void update(int daysElapsed, int eventsTriggered, int deathCount) {
        aggression = (daysElapsed / 5) + (eventsTriggered * 2) + (deathCount * 3);

        if (aggression < 10) {
            currentLevel = Level.DORMANT;
        } else if (aggression < 25) {
            currentLevel = Level.AWARE;
        } else if (aggression < 50) {
            currentLevel = Level.ALERT;
        } else {
            currentLevel = Level.HOSTILE;
        }

        trapComplexity = Math.min(10, aggression / 5);
        aiIntelligence = Math.min(1.0f, aggression / 100.0f);
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public int getTrapComplexity() {
        return trapComplexity;
    }

    public float getAIIntelligence() {
        return aiIntelligence;
    }

    public int getAggression() {
        return aggression;
    }

    public float getIntensity() {
        return currentLevel.intensity;
    }

    public boolean shouldSpawnTraps() {
        return trapComplexity > 0;
    }

    public boolean shouldEnhanceRedstone() {
        return trapComplexity > 3;
    }

    public int getMaxSimultaneousTraps() {
        return Math.max(1, trapComplexity / 2);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("currentLevel", currentLevel.name());
        tag.putInt("aggression", aggression);
        tag.putInt("trapComplexity", trapComplexity);
        tag.putFloat("aiIntelligence", aiIntelligence);
        return tag;
    }

    public void load(CompoundTag tag) {
        try {
            currentLevel = Level.valueOf(tag.getString("currentLevel"));
        } catch (Exception e) {
            currentLevel = Level.DORMANT;
        }
        aggression = tag.getInt("aggression");
        trapComplexity = tag.getInt("trapComplexity");
        aiIntelligence = tag.getFloat("aiIntelligence");
    }
}
