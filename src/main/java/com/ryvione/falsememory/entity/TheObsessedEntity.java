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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

public class TheObsessedEntity extends PathfinderMob {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<String> TARGET_PLAYER_UUID =
        SynchedEntityData.defineId(TheObsessedEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> BEHAVIOR_PHASE =
        SynchedEntityData.defineId(TheObsessedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MIMIC_YAW =
        SynchedEntityData.defineId(TheObsessedEntity.class, EntityDataSerializers.FLOAT);

    private UUID targetPlayerUUID = null;
    private int ticksSinceLastMove = 0;
    private BlockPos lastKnownPlayerPos = null;
    private int ritualProgress = 0;
    private boolean firstSightingGranted = false;

    public TheObsessedEntity(EntityType<? extends TheObsessedEntity> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal(""));
        this.setCustomNameVisible(false);
        this.setSilent(true);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_PLAYER_UUID, "");
        builder.define(BEHAVIOR_PHASE, 0);
        builder.define(MIMIC_YAW, 0.0f);
    }

    public String getTargetPlayerUUID() {
        return entityData.get(TARGET_PLAYER_UUID);
    }

    public int getBehaviorPhase() {
        return entityData.get(BEHAVIOR_PHASE);
    }

    public float getMimicYaw() {
        return entityData.get(MIMIC_YAW);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (targetPlayerUUID == null) {
            if (++ticksSinceLastMove > 1200) this.discard();
            return;
        }

        Entity targetEntity = this.level().getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (!(targetEntity instanceof ServerPlayer target)) {
            if (++ticksSinceLastMove > 6000) this.discard();
            return;
        }

        PlayerMemory memory = MemoryManager.getInstance() != null
            ? MemoryManager.getInstance().getOrCreate(target) : null;

        int phase = memory != null ? memory.knowledgeTier : 0;
        entityData.set(BEHAVIOR_PHASE, phase);

        boolean watched = isPlayerLookingAtMe(target);
        if (watched && phase < 3) {
            faceEntity(target);
            return;
        }

        switch (phase) {
            case 0 -> behaviorPhase0(target);
            case 1 -> behaviorPhase1(target);
            case 2 -> behaviorPhase2(target, memory);
            case 3 -> behaviorPhase3(target, memory);
        }

        ticksSinceLastMove++;
        if (!firstSightingGranted) {
            firstSightingGranted = true;
            com.ryvione.falsememory.advancement.AdvancementTriggers.grant(target,
                com.ryvione.falsememory.advancement.AdvancementTriggers.FIRST_SIGHTING);
        }
    }

    private void behaviorPhase0(ServerPlayer target) {
        if (this.distanceTo(target) < 55) teleportToEdge(target, 65);
        faceEntity(target);
    }

    private void behaviorPhase1(ServerPlayer target) {
        double dist = this.distanceTo(target);
        if (dist > 80) teleportToEdge(target, 60);
        else if (dist < 40) teleportToEdge(target, 55);

        if (ticksSinceLastMove % 200 == 0 && lastKnownPlayerPos != null) {
            BlockPos ghost = lastKnownPlayerPos;
            if (target.blockPosition().distSqr(ghost) > 100) {
                this.teleportTo(ghost.getX() + 0.5, ghost.getY(), ghost.getZ() + 0.5);
            }
        }
        lastKnownPlayerPos = target.blockPosition();
        faceEntity(target);
    }

    private void behaviorPhase2(ServerPlayer target, PlayerMemory memory) {
        if (memory != null) entityData.set(MIMIC_YAW, memory.getDominantFacingYaw());

        if (memory != null && memory.inferredHomePos != null && this.distanceTo(target) > 30) {
            BlockPos home = memory.inferredHomePos;
            this.getNavigation().moveTo(home.getX(), home.getY(), home.getZ(), 0.8);
        }

        if (this.distanceTo(target) < 20) teleportToEdge(target, 45);
        lastKnownPlayerPos = target.blockPosition();
    }

    private void behaviorPhase3(ServerPlayer target, PlayerMemory memory) {
        double dist = this.distanceTo(target);
        if (dist > 5 && ticksSinceLastMove % 20 == 0) {
            this.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 0.4);
        }
        if (dist < 6) {
            this.getNavigation().stop();
            faceEntity(target);
            checkRitualItems(target, memory);
        }
        faceEntity(target);
        lastKnownPlayerPos = target.blockPosition();
    }

    private void checkRitualItems(ServerPlayer player, PlayerMemory memory) {
        if (ritualProgress > 0) {
            ritualProgress++;
            if (player.blockPosition().distSqr(this.blockPosition()) > 25) {
                ritualProgress = 0;
                player.sendSystemMessage(Component.literal("§7The connection breaks."));
                return;
            }
            if (ritualProgress >= 100) completeRitual(player, memory);
            return;
        }

        boolean hasBook = false, hasWool = false, hasStone = false;
        for (int i = 0; i < 9; i++) {
            var item = player.getInventory().getItem(i);
            if (item.getItem() == Items.WRITTEN_BOOK) hasBook = true;
            if (item.getItem() == Items.WHITE_WOOL) hasWool = true;
            if (item.getItem() == Items.COBBLESTONE || item.getItem() == Items.STONE) hasStone = true;
        }

        if (hasBook && hasWool && hasStone) {
            ritualProgress = 1;
            player.sendSystemMessage(Component.literal("§7Something shifts..."));
        }
    }

    private void completeRitual(ServerPlayer player, PlayerMemory memory) {
        if (memory != null) {
            memory.knowledgeTier = 0;
            memory.triggeredEvents.clear();
            memory.worldDayCount = 0;
            memory.inManhunt = false;
        }

        for (int i = 0; i < 9; i++) {
            var item = player.getInventory().getItem(i);
            if (item.getItem() == Items.WRITTEN_BOOK ||
                item.getItem() == Items.WHITE_WOOL ||
                item.getItem() == Items.COBBLESTONE) {
                item.shrink(1);
            }
        }

        player.playSound(net.minecraft.sounds.SoundEvents.TOTEM_USE, 0.5f, 0.3f);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(20, 80, 40));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
            Component.literal("§0It's over.")));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
            Component.literal("§8...for now.")));
            com.ryvione.falsememory.advancement.AdvancementTriggers.grant(player,
            com.ryvione.falsememory.advancement.AdvancementTriggers.RITUAL_COMPLETE);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer player) {
            player.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_HURT, 0.6f, 0.4f);
        }
        return false;
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isInvulnerable() { return true; }

    private void teleportToEdge(ServerPlayer target, double distance) {
        double angle = this.random.nextDouble() * Math.PI * 2;
        double tx = target.getX() + Math.sin(angle) * distance;
        double tz = target.getZ() + Math.cos(angle) * distance;
        BlockPos pos = new BlockPos((int) tx, (int) target.getY(), (int) tz);
        int attempts = 0;
        while (this.level().getBlockState(pos).isAir() && pos.getY() > 0 && attempts++ < 10) pos = pos.below();
        this.teleportTo(tx, pos.getY() + 1, tz);
        faceEntity(target);
    }

    private void faceEntity(Entity target) {
        Vec3 diff = target.position().subtract(this.position()).normalize();
        float yaw = (float) (Math.toDegrees(Math.atan2(-diff.x, diff.z)));
        this.setYRot(yaw);
        this.yHeadRot = yaw;
        entityData.set(MIMIC_YAW, yaw);
    }

    private boolean isPlayerLookingAtMe(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 toEntity = this.position().subtract(player.position()).normalize();
        return look.dot(toEntity) > 0.95 && this.distanceTo(player) < 60;
    }

    public void setTargetPlayer(UUID playerUUID) {
        this.targetPlayerUUID = playerUUID;
        entityData.set(TARGET_PLAYER_UUID, playerUUID.toString());
    }

    public UUID getTargetPlayerUUID_Raw() { return targetPlayerUUID; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayerUUID != null) tag.putUUID("TargetPlayer", targetPlayerUUID);
        tag.putInt("RitualProgress", ritualProgress);
        tag.putInt("TicksSinceLastMove", ticksSinceLastMove);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            targetPlayerUUID = tag.getUUID("TargetPlayer");
            entityData.set(TARGET_PLAYER_UUID, targetPlayerUUID.toString());
        }
        ritualProgress = tag.getInt("RitualProgress");
        ticksSinceLastMove = tag.getInt("TicksSinceLastMove");
    }
}