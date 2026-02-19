package com.ryvione.falsememory.ai;

import net.minecraft.nbt.CompoundTag;

public class PlayerProfile {

    public enum ProfileType {
        MINER, BUILDER, EXPLORER, FIGHTER, HYBRID
    }

    public ProfileType profileType = ProfileType.HYBRID;
    public int miningScore = 0;
    public int buildingScore = 0;
    public int exploringScore = 0;
    public int fightingScore = 0;
    public int confidenceLevel = 0;

    public void updateProfile(int blocksBroken, int blocksPlaced, int biomesVisited, int combatsWon) {
        miningScore = blocksBroken;
        buildingScore = blocksPlaced;
        exploringScore = biomesVisited;
        fightingScore = combatsWon;

        int maxScore = Math.max(Math.max(miningScore, buildingScore), Math.max(exploringScore, fightingScore));
        if (maxScore == 0) {
            profileType = ProfileType.HYBRID;
            return;
        }

        if (miningScore == maxScore && miningScore > buildingScore && miningScore > exploringScore) {
            profileType = ProfileType.MINER;
        } else if (buildingScore == maxScore && buildingScore > miningScore && buildingScore > exploringScore) {
            profileType = ProfileType.BUILDER;
        } else if (exploringScore == maxScore && exploringScore > miningScore && exploringScore > buildingScore) {
            profileType = ProfileType.EXPLORER;
        } else if (fightingScore == maxScore && fightingScore > miningScore && fightingScore > buildingScore) {
            profileType = ProfileType.FIGHTER;
        } else {
            profileType = ProfileType.HYBRID;
        }

        confidenceLevel = Math.min(100, (miningScore + buildingScore + exploringScore + fightingScore) / 4);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("profileType", profileType.name());
        tag.putInt("miningScore", miningScore);
        tag.putInt("buildingScore", buildingScore);
        tag.putInt("exploringScore", exploringScore);
        tag.putInt("fightingScore", fightingScore);
        tag.putInt("confidenceLevel", confidenceLevel);
        return tag;
    }

    public void load(CompoundTag tag) {
        profileType = ProfileType.valueOf(tag.getString("profileType"));
        miningScore = tag.getInt("miningScore");
        buildingScore = tag.getInt("buildingScore");
        exploringScore = tag.getInt("exploringScore");
        fightingScore = tag.getInt("fightingScore");
        confidenceLevel = tag.getInt("confidenceLevel");
    }
}
