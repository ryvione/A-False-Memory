package com.ryvione.falsememory.entity;

import com.mojang.logging.LogUtils;
import com.ryvione.falsememory.ai.intel.PlayerIntelligence;
import com.ryvione.falsememory.ai.neural.DeepNeuralBrain;
import com.ryvione.falsememory.ai.perception.PerceptionSystem;
import com.ryvione.falsememory.ai.tactics.TacticalSystem;
import com.ryvione.falsememory.entity.swimming.SwimmingGoal;
import com.ryvione.falsememory.events.HorrorEvents;
import com.ryvione.falsememory.memory.MemoryManager;
import com.ryvione.falsememory.memory.PlayerMemory;
import com.ryvione.falsememory.tracking.CombatAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Random;
import java.util.UUID;

public class TheOnlyOneEntity extends PathfinderMob {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG    = new Random();

    private static final EntityDataAccessor<String>  TARGET_UUID_DATA =
        SynchedEntityData.defineId(TheOnlyOneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> INTELLIGENCE_LEVEL_DATA =
        SynchedEntityData.defineId(TheOnlyOneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IN_MANHUNT_DATA =
        SynchedEntityData.defineId(TheOnlyOneEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID  targetPlayerUUID = null;
    private long  ticksAlive       = 0;
    private int   combatTicks      = 0;
    private float baseMoveSpeed    = 0.4f;

    private DeepNeuralBrain  brain         = new DeepNeuralBrain();
    private PerceptionSystem perception    = null;
    private TacticalSystem   tactics       = null;
    private PlayerIntelligence intelligence = new PlayerIntelligence();
    private CombatAnalyzer   combatAnalyzer = null;
    private com.ryvione.falsememory.entity.TrapExploiter trapExploiter = null;

    private int  horrorSoundCooldown       = 0;
    private int  shadowTeleportCooldown    = 0;
    private int  deathSpotWaitCooldown     = 0;
    private int  nearPlayerTicks           = 0;
    private int  intelligenceUpdateCooldown = 0;

    private float lastSeenPlayerHP         = 20f;
    private int   timesPlayerFled          = 0;
    private ItemStack equippedTool         = ItemStack.EMPTY;

    public TheOnlyOneEntity(EntityType<? extends TheOnlyOneEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(false);
        this.setSilent(true);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,          60.0)
            .add(Attributes.MOVEMENT_SPEED,       0.4)
            .add(Attributes.FOLLOW_RANGE,        512.0)
            .add(Attributes.ATTACK_DAMAGE,         5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE,  1.0)
            .add(Attributes.ATTACK_KNOCKBACK,      0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID_DATA, "");
        builder.define(INTELLIGENCE_LEVEL_DATA, 0);
        builder.define(IN_MANHUNT_DATA, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new SwimmingGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
    }

    public void initFromPlayer(ServerPlayer player) {
        this.targetPlayerUUID = player.getUUID();
        entityData.set(TARGET_UUID_DATA, player.getUUID().toString());
        entityData.set(IN_MANHUNT_DATA, true);

        baseMoveSpeed = (float) player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.05f;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
            Math.max(60, player.getMaxHealth() * 3.0));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseMoveSpeed);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
            player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4);

        copyPlayerEquipment(player);

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr != null) {
            PlayerMemory memory = mgr.getOrCreate(player);
            combatAnalyzer = new CombatAnalyzer(player);
            intelligence.attachCombatAnalyzer(combatAnalyzer);
            intelligence.update(player, memory);
        }

        this.perception = new PerceptionSystem(player);
        this.tactics    = new TacticalSystem(this);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            this.trapExploiter = new com.ryvione.falsememory.entity.TrapExploiter(this, player, mgr.getOrCreate(player), sl);
        }
        this.setHealth(this.getMaxHealth());
        lastSeenPlayerHP = player.getHealth();

        LOGGER.info("[FalseMemory] The Only One initialized for {} | confidence={}% | strategy={}",
            player.getName().getString(),
            intelligence.dataConfidence,
            intelligence.bestStrategyAgainstThem.name());
    }

    private void copyPlayerEquipment(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            Item it = mainHand.getItem();
            if (it instanceof SwordItem || it instanceof AxeItem || it instanceof PickaxeItem) {
                this.equippedTool = mainHand.copy();
                this.setItemInHand(InteractionHand.MAIN_HAND, equippedTool);
            }
        }
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ShieldItem) {
            this.setItemInHand(InteractionHand.OFF_HAND, offHand.copy());
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty()) this.setItemSlot(slot, armor.copy());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        ticksAlive++;
        horrorSoundCooldown    = Math.max(0, horrorSoundCooldown - 1);
        shadowTeleportCooldown = Math.max(0, shadowTeleportCooldown - 1);
        deathSpotWaitCooldown  = Math.max(0, deathSpotWaitCooldown - 1);
        intelligenceUpdateCooldown = Math.max(0, intelligenceUpdateCooldown - 1);

        if (targetPlayerUUID == null) return;
        ServerPlayer player = this.level().getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (player == null) return;

        MemoryManager mgr = MemoryManager.getInstance();
        if (mgr == null) return;
        PlayerMemory memory = mgr.getOrCreate(player);

        if (perception == null)     perception = new PerceptionSystem(player);
        if (tactics == null)        tactics    = new TacticalSystem(this);
        if (combatAnalyzer == null) {
            combatAnalyzer = new CombatAnalyzer(player);
            intelligence.attachCombatAnalyzer(combatAnalyzer);
        }

        if (intelligenceUpdateCooldown == 0) {
            intelligence.update(player, memory);
            updateCombatAnalyzer(player);
            intelligenceUpdateCooldown = 20;
        }

        float[] inputs   = perception.perceive(this, player, intelligence);
        float[] decision = brain.predict(inputs);

        if (trapExploiter != null && intelligence.dataConfidence > 40
                && !intelligence.trapAware) {
            double dist = this.distanceTo(player);
            if (dist > 6 && dist < 40 && trapExploiter.shouldLureIntoTrap(dist)) {
                BlockPos lurePos = trapExploiter.findOptimalTrapLure();
                if (lurePos != null) {
                    trapExploiter.lureTowardsTrap(lurePos);
                } else {
                    tactics.executeTactics(decision, player, intelligence);
                }
            } else {
                tactics.executeTactics(decision, player, intelligence);
            }
        } else {
            tactics.executeTactics(decision, player, intelligence);
        }
        updateIntelligenceLevel();

        if (ticksAlive % 8 == 0) {
            trainBrain(player, decision, memory);
        }
        if (ticksAlive % 20 == 0) {
            adaptStats(memory);
            mirrorPlayerEquipment(player);
        }
        if (ticksAlive % 40 == 0) {
            horrorPresence(player, memory);
        }
        if (ticksAlive % 200 == 0) {
            considerDeathSpotWait(player, memory);
        }

        trackPlayerHP(player);
        trackNearPlayer(player);
        if (combatTicks > 0) combatTicks--;
    }

    private void updateCombatAnalyzer(ServerPlayer player) {
        if (combatAnalyzer == null) return;
        Entity currentTarget = this.getTarget();
        combatAnalyzer.recordCombatTick(currentTarget != null ? currentTarget : player);
    }

    private void trainBrain(ServerPlayer player, float[] decision, PlayerMemory memory) {
        float myHPRatio = this.getHealth() / this.getMaxHealth();
        float tgHP      = player.getHealth();
        float tgMaxHP   = player.getMaxHealth();
        double dist     = this.distanceTo(player);

        if (tgHP > lastSeenPlayerHP + 1f) timesPlayerFled++;

        float[] targets = new float[DeepNeuralBrain.OUTPUT_NODES];

        targets[0] = myHPRatio > 0.4f ? 1f : 0.1f;                    
        targets[1] = myHPRatio < 0.25f ? 1f : 0f;                     
        targets[2] = clamp(1.0 - dist / 256.0);                       
        targets[3] = tactics.getConsecutiveHits() < 2 ? 0.8f : 0.3f;  
        targets[4] = player.isSprinting() ? 0.7f : 0.3f;              
        targets[5] = this.isInWater() ? 1f : 0f;                      
        targets[6] = clamp(dist / 30.0);                               
        targets[7] = intelligence.dataConfidence / 100f;               
        targets[8] = (timesPlayerFled > 2) ? 0.9f : 0.3f;            
        targets[9] = dist > 30 && dist < 80 ? 0.7f : 0.2f;           
        targets[10] = intelligence.trapAware ? 0.6f : 0.2f;           
        targets[11] = myHPRatio < 0.2f ? 1f : 0f;                    

        brain.train(targets);
        lastSeenPlayerHP = tgHP;
    }

    private void adaptStats(PlayerMemory memory) {
        
        double confMult = 1.0 + (intelligence.dataConfidence / 100.0) * 0.2;

        if (intelligence.tendsToFlee) {
            double dmg = this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.min(dmg * 1.0005, 28.0));
        }

        if (intelligence.adaptability > 0.6f || memory.combatsFled > 5) {
            double spd = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                Math.min(spd * 1.0003, baseMoveSpeed * 1.3));
        }
    }

    private void updateIntelligenceLevel() {
        int tc = brain.getTrainingCount();
        int level;
        if      (tc < 100)  level = 0;
        else if (tc < 500)  level = 1;
        else if (tc < 1500) level = 2;
        else if (tc < 4000) level = 3;
        else                level = 4;
        entityData.set(INTELLIGENCE_LEVEL_DATA, level);
    }

    private void mirrorPlayerEquipment(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            Item it = mainHand.getItem();
            if ((it instanceof SwordItem || it instanceof AxeItem || it instanceof PickaxeItem)
                    && !ItemStack.isSameItem(equippedTool, mainHand)) {
                equippedTool = mainHand.copy();
                this.setItemInHand(InteractionHand.MAIN_HAND, equippedTool);
            }
        }
    }

    private void horrorPresence(ServerPlayer player, PlayerMemory memory) {
        if (horrorSoundCooldown > 0) return;
        double dist = this.distanceTo(player);

        if (intelligence.caution > 0.6f && dist > 20 && dist < 80) {
            if (RNG.nextFloat() < 0.25f) {
                HorrorEvents.playSoundForPlayer(player, "minecraft:block.stone.step",
                    SoundSource.PLAYERS, 0.25f, 0.75f + RNG.nextFloat() * 0.3f);
                horrorSoundCooldown = 80;
                return;
            }
        }

        if (intelligence.boldness > 0.6f && dist < 12) {
            if (RNG.nextFloat() < 0.2f) {
                HorrorEvents.playSoundForPlayer(player, "minecraft:entity.enderman.stare",
                    SoundSource.HOSTILE, 0.45f, 0.5f);
                horrorSoundCooldown = 100;
                return;
            }
        }

        if (intelligence.panicsAtLowHP && player.getHealth() / player.getMaxHealth() < 0.3f) {
            HorrorEvents.playSoundForPlayer(player, "minecraft:entity.wither.ambient",
                SoundSource.HOSTILE, 0.5f, 0.4f);
            horrorSoundCooldown = 60;
        }

        if (dist > 100 && dist < 300 && shadowTeleportCooldown == 0) {
            if (RNG.nextFloat() < 0.12f) {
                performShadowTeleport(player);
                shadowTeleportCooldown = 600;
                horrorSoundCooldown = 200;
            }
        }
    }

    private void considerDeathSpotWait(ServerPlayer player, PlayerMemory memory) {
        if (deathSpotWaitCooldown > 0) return;
        BlockPos deathSpot = intelligence.getMostRecentDeathSpot();
        if (deathSpot == null) return;

        double distToSpot = player.blockPosition().distSqr(deathSpot);
        
        if (distToSpot < 10000 && distToSpot > 400) {
            double myDistToSpot = this.blockPosition().distSqr(deathSpot);
            
            if (myDistToSpot < distToSpot * 2) {
                this.getNavigation().moveTo(deathSpot.getX(), deathSpot.getY(), deathSpot.getZ(), 1.4);
                deathSpotWaitCooldown = 1200;
            }
        }
    }

    private void performShadowTeleport(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel sLevel)) return;
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist  = 12 + RNG.nextInt(8);
        double tx = player.getX() + Math.sin(angle) * dist;
        double tz = player.getZ() + Math.cos(angle) * dist;
        BlockPos tp = BlockPos.containing(tx, player.getY(), tz);
        for (int attempts = 0; attempts < 10 && sLevel.getBlockState(tp).isAir() && tp.getY() > 0; attempts++) {
            tp = tp.below();
        }
        tp = tp.above();
        this.teleportTo(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5);
    }

    private void trackPlayerHP(ServerPlayer player) {
        float currentHP = player.getHealth();
        if (currentHP > lastSeenPlayerHP + 2f) timesPlayerFled++;
        lastSeenPlayerHP = currentHP;
    }

    private void trackNearPlayer(ServerPlayer player) {
        if (this.distanceTo(player) < 8) nearPlayerTicks++;
        else nearPlayerTicks = Math.max(0, nearPlayerTicks - 1);
    }

    private float clamp(double v) {
        return (float) Math.min(1.0, Math.max(0.0, v));
    }

    public void notifyCombatHit(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (combatAnalyzer != null) {
            combatAnalyzer.recordCombatTick(this.getTarget());
        }
        
        if (targetPlayerUUID != null) {
            MemoryManager mgr = MemoryManager.getInstance();
            if (mgr != null) {
                PlayerMemory memory = mgr.getOrCreate(targetPlayerUUID);
                
                if (memory.averageHealthWhenFleeing > 0) {
                    intelligence.fleeHealthThreshold = memory.averageHealthWhenFleeing / 20f;
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);
        if (target instanceof ServerPlayer) {
            combatTicks = 100;
            if (tactics != null) tactics.onHitTarget();
        }
        return result;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer) {
            combatTicks = 120;
            if (tactics != null) tactics.onMissTarget();
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayerUUID != null) tag.putUUID("targetPlayer", targetPlayerUUID);
        tag.putLong("ticksAlive", ticksAlive);
        tag.putInt("combatTicks", combatTicks);
        tag.putFloat("lastSeenHP", lastSeenPlayerHP);
        tag.putInt("timesPlayerFled", timesPlayerFled);
        tag.put("brain", brain.save());
        tag.put("intelligence", intelligence.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("targetPlayer")) {
            this.targetPlayerUUID = tag.getUUID("targetPlayer");
            entityData.set(TARGET_UUID_DATA, targetPlayerUUID.toString());
        }
        ticksAlive      = tag.getLong("ticksAlive");
        combatTicks     = tag.getInt("combatTicks");
        lastSeenPlayerHP = tag.getFloat("lastSeenHP");
        timesPlayerFled  = tag.getInt("timesPlayerFled");
        if (tag.contains("brain"))        brain.load(tag.getCompound("brain"));
        if (tag.contains("intelligence")) intelligence.load(tag.getCompound("intelligence"));
        entityData.set(IN_MANHUNT_DATA, targetPlayerUUID != null);
        entityData.set(INTELLIGENCE_LEVEL_DATA, Math.min(4, brain.getTrainingCount() / 500));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!(this.level() instanceof ServerLevel sl)) return;

        ServerPlayer player = targetPlayerUUID != null
            ? sl.getServer().getPlayerList().getPlayer(targetPlayerUUID) : null;

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag bookTag = new CompoundTag();
        bookTag.putString("title", "Error Log");
        bookTag.putString("author", "???");
        ListTag pages = new ListTag();

        String trainStr = String.valueOf(brain.getTrainingCount());
        String lossStr  = String.format("%.4f", brain.getRecentLoss());

        String combatLine = player != null
            ? "Target: " + player.getName().getString() + "\nFled: " + (player != null ? "unknown" : "unknown")
            : "Target: [disconnected]";

        pages.add(StringTag.valueOf(
            net.minecraft.network.chat.Component.Serializer.toJson(
                net.minecraft.network.chat.Component.literal(
                    "Training cycles: " + trainStr +
                    "\nFinal loss: " + lossStr +
                    "\n\nI miscalculated your dodge.\n\nI won't again."
                ), RegistryAccess.EMPTY)));

        if (player != null) {
            float fleeHP = 0f;
            MemoryManager mgr = MemoryManager.getInstance();
            if (mgr != null) {
                PlayerMemory mem = mgr.getOrCreate(player);
                fleeHP = mem.averageHealthWhenFleeing / 2f;
            }
            pages.add(StringTag.valueOf(
                net.minecraft.network.chat.Component.Serializer.toJson(
                    net.minecraft.network.chat.Component.literal(
                        "You flee at " + String.format("%.1f", fleeHP) + " hearts.\n\n" +
                        "I had that memorized.\n\nSomething was wrong\nwith my inputs.\n\nNext time."
                    ), RegistryAccess.EMPTY)));
        }

        bookTag.put("pages", pages);
        book.applyComponents(DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_DATA, CustomData.of(bookTag)).build());

        this.spawnAtLocation(book);

        if (player != null) {
            com.ryvione.falsememory.ending.EndingManager.triggerEnding(
                player, com.ryvione.falsememory.ending.EndingType.VICTORY);
        }
    }

    @Override public boolean canPickUpLoot()            { return false; }
    @Override protected boolean shouldDespawnInPeaceful(){ return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    public String getTargetUUID()        { return entityData.get(TARGET_UUID_DATA); }
    public boolean isInManhunt()         { return entityData.get(IN_MANHUNT_DATA); }
    public int getIntelligenceLevel()    { return entityData.get(INTELLIGENCE_LEVEL_DATA); }
    public int getBrainTrainingCount()   { return brain.getTrainingCount(); }
    public float getRecentBrainLoss()    { return brain.getRecentLoss(); }
    public PlayerIntelligence getIntel() { return intelligence; }
    public TacticalSystem.ActiveTactic getCurrentTactic() {
        return tactics != null ? tactics.getCurrentTactic() : TacticalSystem.ActiveTactic.STALKING;
    }
}
