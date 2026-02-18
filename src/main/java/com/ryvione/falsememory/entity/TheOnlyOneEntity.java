package com.ryvione.falsememory.entity;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.events.HorrorEvents;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.CombatAnalyzer;
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
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

public class TheOnlyOneEntity extends PathfinderMob {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<String> TARGET_UUID =
        SynchedEntityData.defineId(TheOnlyOneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> COMBAT_PHASE =
        SynchedEntityData.defineId(TheOnlyOneEntity.class, EntityDataSerializers.INT);

    private UUID targetPlayerUUID = null;
    private boolean inManhunt = false;
    private int adaptTimer = 0;
    private int ticksAlive = 0;
    private boolean usesRanged = false;
    private boolean tendsFlee = false;
    private float fleeHealthThreshold = 0.3f;
    private String preferredWeapon = "sword";

    private CombatAnalyzer combatAnalyzer = null;
    private int inCombatTicks = 0;
    private static final int COMBAT_TIMEOUT = 600; 

    private static final int PHASE_NORMAL = 0;
    private static final int PHASE_RANGED = 1;
    private static final int PHASE_FLANKING = 2;
    private static final int PHASE_MANHUNT = 3;
    private static final int PHASE_PURSUIT = 4;

    public TheOnlyOneEntity(EntityType<? extends TheOnlyOneEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(false);
        this.setSilent(true);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 128.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID, "");
        builder.define(COMBAT_PHASE, PHASE_NORMAL);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void initFromPlayer(ServerPlayer player) {
        this.targetPlayerUUID = player.getUUID();
        entityData.set(TARGET_UUID, player.getUUID().toString());

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
            Math.max(3.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE)));

        PlayerMemory memory = MemoryManager.getInstance() != null
            ? MemoryManager.getInstance().getOrCreate(player) : null;

        if (memory != null) {
            usesRanged = memory.usesRangedWeapons;
            tendsFlee = memory.tendsToFlee();
            fleeHealthThreshold = memory.averageHealthWhenFleeing / player.getMaxHealth();
            preferredWeapon = memory.preferredWeaponType;

            equipFromPlayer(player, memory);
        }

        this.combatAnalyzer = new CombatAnalyzer(player);

        this.setHealth(this.getMaxHealth());
        LOGGER.info("[FalseMemory] The Only One initialized from player {}", player.getName().getString());
    }

    private void equipFromPlayer(ServerPlayer player, PlayerMemory memory) {
        var hotbar = memory.lastHotbarSnapshot;
        if (!hotbar.isEmpty()) {
            for (String itemId : hotbar) {
                var itemOpt = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(net.minecraft.resources.ResourceLocation.parse(itemId));
                itemOpt.ifPresent(item -> {
                    if (item instanceof SwordItem || item instanceof AxeItem) {
                        this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(item));
                    }
                });
            }
        }

        for (int i = 0; i < 4; i++) {
            var armor = player.getInventory().armor.get(i);
            if (!armor.isEmpty()) {
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        ticksAlive++;

        if (targetPlayerUUID == null) return;
        Entity target = this.level().getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (!(target instanceof ServerPlayer player)) return;

        PlayerMemory memory = MemoryManager.getInstance() != null
            ? MemoryManager.getInstance().getOrCreate(player) : null;

        if (inManhunt) {
            tickManhunt(player, memory);
            return;
        }

        double dist = this.distanceTo(player);

        if (dist > 200) {
            activateManhunt(player, memory);
            return;
        }

        if (combatAnalyzer != null) {
            combatAnalyzer.recordCombatTick(player);
            inCombatTicks++;
        } else {
            inCombatTicks = 0;
        }

        if (dist > 30) {
            inCombatTicks = 0;
            if (combatAnalyzer != null) combatAnalyzer.reset();
        }

        adaptTimer++;
        if (adaptTimer >= 100) { 
            adaptTimer = 0;
            adaptTacticsFromCombat(player, memory);
        }

        int currentPhase = entityData.get(COMBAT_PHASE);

        switch (currentPhase) {
            case PHASE_NORMAL -> tickPhaseNormal(player, dist);
            case PHASE_RANGED -> tickPhaseRanged(player, dist);
            case PHASE_FLANKING -> tickPhaseFlanking(player, dist);
            case PHASE_PURSUIT -> tickPhasePursuit(player, dist);
        }

        checkSelfFlee(player);
    }


    private void adaptTacticsFromCombat(ServerPlayer player, PlayerMemory memory) {
        if (combatAnalyzer == null || inCombatTicks < 20) return; 

        CombatAnalyzer.CombatStrategy strategy = combatAnalyzer.getSuggestedStrategy();

        switch (strategy) {
            case AGGRESSIVE_MELEE -> {
                entityData.set(COMBAT_PHASE, PHASE_NORMAL);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
            }
            case RANGED_KITE -> {
                entityData.set(COMBAT_PHASE, PHASE_RANGED);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
            }
            case AGGRESSIVE_FLANKING -> {
                entityData.set(COMBAT_PHASE, PHASE_FLANKING);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38);
            }
            case PURSUIT -> {
                entityData.set(COMBAT_PHASE, PHASE_PURSUIT);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
            }
            case ADAPTIVE -> {
                int weakest = getWeakestPlayerStrategy();
                entityData.set(COMBAT_PHASE, weakest);
            }
        }

        LOGGER.debug("[FalseMemory] Adapted to strategy: {}", strategy);
    }

    private int getWeakestPlayerStrategy() {
        if (combatAnalyzer == null) return PHASE_NORMAL;

        int sprint = combatAnalyzer.getSprintAttackCount();
        int standing = combatAnalyzer.getStandsGroundCount();
        int circle = combatAnalyzer.getCircleStrafesCount();

        if (circle < 2) return PHASE_FLANKING;
        if (standing < 2) return PHASE_RANGED;
        return PHASE_NORMAL;
    }

    private void tickPhaseNormal(ServerPlayer player, double dist) {
        if (dist > 3) {
            Vec3 predictedPos = combatAnalyzer != null ?
                combatAnalyzer.predictPlayerMovement(10) : player.position();
            this.getNavigation().moveTo(predictedPos.x, predictedPos.y, predictedPos.z, 1.0);
        }
        if (dist < 2.5 && ticksAlive % 15 == 0) {
            this.doHurtTarget(player);
        }
    }

    private void tickPhaseRanged(ServerPlayer player, double dist) {
        if (dist < 12) {
            Vec3 away = this.position().subtract(player.position()).normalize().scale(6);
            this.getNavigation().moveTo(
                this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.2);
        } else if (dist > 25) {
            this.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.0);
        }

        if (dist < 30 && ticksAlive % 25 == 0) {
            shootArrowAt(player);
        }
    }

    private void tickPhaseFlanking(ServerPlayer player, double dist) {
        Vec3 playerLook = player.getLookAngle();
        Vec3 flankDir = new Vec3(-playerLook.z, 0, playerLook.x).normalize().scale(8);
        Vec3 flankTarget = player.position().add(flankDir);
        this.getNavigation().moveTo(flankTarget.x, flankTarget.y, flankTarget.z, 1.1);

        if (dist < 3 && ticksAlive % 18 == 0) {
            this.doHurtTarget(player);
        }
    }


    private void tickPhasePursuit(ServerPlayer player, double dist) {
        this.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.3);

        if (dist < 3) {
            this.doHurtTarget(player);
        }

        if (player.getY() > this.getY()) {
            this.getNavigation().moveTo(
                player.getX(), player.getY() + 2, player.getZ(), 1.3);
        }
    }

    private void checkSelfFlee(ServerPlayer player) {
        float healthPercent = this.getHealth() / this.getMaxHealth();


        if (tendsFlee && healthPercent < fleeHealthThreshold) {

            if (ticksAlive % 60 == 0) {
                this.getLookControl().setLookAt(player, 10f, 10f);
            }
        }
    }

    private void activateManhunt(ServerPlayer player, PlayerMemory memory) {
        inManhunt = true;
        entityData.set(COMBAT_PHASE, PHASE_PURSUIT);

        if (memory != null) memory.inManhunt = true;

        double speed = com.ryvione.falsememory.Config.INSTANCE.manhuntSpeedMultiplier.get();
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.45 * speed);

        HorrorEvents.sendTitle(player, "§4It's not over.", "§8Run.", 10, 60, 20);
        HorrorEvents.playSoundForPlayer(player, "minecraft:entity.wither.spawn",
            net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 0.4f);

        LOGGER.info("[FalseMemory] Manhunt activated for {}", player.getName().getString());
    }

    private void tickManhunt(ServerPlayer player, PlayerMemory memory) {
        double dist = this.distanceTo(player);

        if (dist > 5) {
            Vec3 predictedPos = combatAnalyzer != null ?
                combatAnalyzer.predictPlayerMovement(15) : player.position();
            this.getNavigation().moveTo(predictedPos.x, predictedPos.y, predictedPos.z, 1.5);
        } else {
            if (ticksAlive % 15 == 0) this.doHurtTarget(player);
        }

        if (ticksAlive % 1200 == 0) {
            HorrorEvents.playSoundForPlayer(player, "minecraft:entity.enderman.stare",
                net.minecraft.sounds.SoundSource.HOSTILE, 0.5f, 0.3f);
        }
    }

    private void shootArrowAt(ServerPlayer player) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        net.minecraft.world.entity.projectile.Arrow arrow =
            new net.minecraft.world.entity.projectile.Arrow(serverLevel, this, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.setPos(this.getX(), this.getEyeY(), this.getZ());
        Vec3 dir = player.position().add(0, player.getBbHeight() * 0.5, 0)
            .subtract(this.position()).normalize();
        arrow.shoot(dir.x, dir.y, dir.z, 1.6f, 1.0f);
        serverLevel.addFreshEntity(arrow);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide && this.getHealth() <= 0) {
            onDefeated(source);
        }
        return result;
    }

    private void onDefeated(DamageSource source) {
        if (targetPlayerUUID == null) return;
        Entity killer = source.getEntity();
        if (!(killer instanceof ServerPlayer player)) return;

        PlayerMemory memory = MemoryManager.getInstance() != null
            ? MemoryManager.getInstance().getOrCreate(player) : null;

        String finalWords = "...";
        if (memory != null && !memory.chatHistory.isEmpty()) {
            finalWords = memory.chatHistory.get(
                this.random.nextInt(memory.chatHistory.size()));
        }

        player.sendSystemMessage(Component.literal("§8\"" + finalWords + "\""));
        HorrorEvents.sendTitle(player, "", "§8It's over.", 20, 100, 40);
        HorrorEvents.playSoundForPlayer(player, "minecraft:entity.wither.death",
            net.minecraft.sounds.SoundSource.HOSTILE, 0.3f, 0.6f);

        if (memory != null) {
            memory.inManhunt = false;
            memory.knowledgeTier = 0;
            memory.triggeredEvents.clear();
        }

        LOGGER.info("[FalseMemory] The Only One was defeated by {}", player.getName().getString());
    }

    public String getTargetUUID() {
        return entityData.get(TARGET_UUID);
    }

    public boolean isInManhunt() { return inManhunt; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayerUUID != null) tag.putUUID("TargetPlayer", targetPlayerUUID);
        tag.putBoolean("InManhunt", inManhunt);
        tag.putInt("TicksAlive", ticksAlive);
        tag.putString("PreferredWeapon", preferredWeapon);
        tag.putBoolean("UsesRanged", usesRanged);
        tag.putBoolean("TendsFlee", tendsFlee);
        tag.putFloat("FleeThreshold", fleeHealthThreshold);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            targetPlayerUUID = tag.getUUID("TargetPlayer");
            entityData.set(TARGET_UUID, targetPlayerUUID.toString());
        }
        inManhunt = tag.getBoolean("InManhunt");
        ticksAlive = tag.getInt("TicksAlive");
        preferredWeapon = tag.getString("PreferredWeapon");
        usesRanged = tag.getBoolean("UsesRanged");
        tendsFlee = tag.getBoolean("TendsFlee");
        fleeHealthThreshold = tag.getFloat("FleeThreshold");
    }
}