package com.ryvione.falsememory.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class PlayerCloneEntity extends PathfinderMob {

    private Player targetPlayer;
    private int lastMistakeTick = 0;
    private boolean isReplayingMistake = false;

    public PlayerCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            p -> p != targetPlayer));
    }

    public void initFromPlayer(Player player) {
        this.targetPlayer = player;
        this.setPos(player.getX(), player.getY(), player.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (targetPlayer != null && targetPlayer.isAlive()) {
            if (lastMistakeTick % 40 == 0) {
                replicatePastMistake();
            }
            lastMistakeTick++;
        }
    }

    private void replicatePastMistake() {
        if (targetPlayer == null) return;

        if (!isReplayingMistake && this.getRandom().nextDouble() < 0.3) {
            isReplayingMistake = true;

            if (this.getRandom().nextDouble() < 0.5) {
                this.setPos(this.getX() - 5, this.getY(), this.getZ());
            } else {
                this.swing(InteractionHand.MAIN_HAND, true);
            }
        } else {
            isReplayingMistake = false;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayer != null) {
            tag.putUUID("targetPlayer", targetPlayer.getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }
}
