package com.ryvione.falsememory.entity;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

public class TheWitnessEntity extends PathfinderMob {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<String> TARGET_UUID =
        SynchedEntityData.defineId(TheWitnessEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DEATH_COUNT_DISPLAYED =
        SynchedEntityData.defineId(TheWitnessEntity.class, EntityDataSerializers.INT);

    private UUID targetPlayerUUID = null;
    private int ticksAlive = 0;
    private int fingerCountTick = 0;

    public TheWitnessEntity(EntityType<? extends TheWitnessEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(false);
        this.setSilent(true);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID, "");
        builder.define(DEATH_COUNT_DISPLAYED, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        ticksAlive++;

        if (targetPlayerUUID == null) {
            if (ticksAlive > 2400) this.discard();
            return;
        }

        Entity targetEntity = this.level().getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (!(targetEntity instanceof ServerPlayer target)) {
            if (ticksAlive > 6000) this.discard();
            return;
        }

        PlayerMemory memory = MemoryManager.getInstance() != null
            ? MemoryManager.getInstance().getOrCreate(target) : null;

        if (memory != null) {
            entityData.set(DEATH_COUNT_DISPLAYED, memory.totalDeaths);
        }

        faceEntity(target);

        fingerCountTick++;
        if (fingerCountTick % 60 == 0 && memory != null) {
            int deaths = memory.totalDeaths;
            if (deaths > 0 && fingerCountTick / 60 <= deaths) {
                target.sendSystemMessage(Component.literal(
                    "§8" + (fingerCountTick / 60) + " of " + deaths + "..."));
            }
        }

        if (ticksAlive > 24000) this.discard();
    }

    private void faceEntity(Entity target) {
        Vec3 diff = target.position().subtract(this.position()).normalize();
        float yaw = (float)(Math.toDegrees(Math.atan2(-diff.x, diff.z)));
        this.setYRot(yaw);
        this.yHeadRot = yaw;
    }

    public void setTargetPlayer(UUID uuid) {
        this.targetPlayerUUID = uuid;
        entityData.set(TARGET_UUID, uuid.toString());
    }

    public String getTargetUUID() { return entityData.get(TARGET_UUID); }
    public int getDeathCountDisplayed() { return entityData.get(DEATH_COUNT_DISPLAYED); }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isInvulnerable() { return true; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayerUUID != null) tag.putUUID("TargetPlayer", targetPlayerUUID);
        tag.putInt("TicksAlive", ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            targetPlayerUUID = tag.getUUID("TargetPlayer");
            entityData.set(TARGET_UUID, targetPlayerUUID.toString());
        }
        ticksAlive = tag.getInt("TicksAlive");
    }
}