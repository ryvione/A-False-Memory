package com.ryvione.falsememory.ai.tactics;

import com.ryvione.falsememory.ai.intel.PlayerIntelligence;
import com.ryvione.falsememory.tracking.CombatAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TacticalSystem {

    public enum ActiveTactic {
        STALKING, AMBUSH_SETUP, AMBUSH_STRIKE,
        AGGRESSIVE, FLANKING, EVASION,
        INTERCEPT, DEATHSPOT_WAIT, MINING_AMBUSH,
        PURSUIT, RETREAT, SWIMMING
    }

    private final PathfinderMob self;
    private final Random rng = new Random();

    private ActiveTactic currentTactic    = ActiveTactic.STALKING;
    private int  tacticLockTicks          = 0;
    private int  dashCooldown             = 0;
    private int  ambushWaitTicks          = 0;
    private boolean isAmbushing           = false;
    private BlockPos ambushPos            = null;
    private float stalkAngle              = 0f;
    private int  consecutiveHits          = 0;
    private int  consecutiveMisses        = 0;
    private int  circleDirection          = 1;
    private int  directionChangeCooldown  = 0;

    private final float[] tacticScores    = new float[ActiveTactic.values().length];
    private int  lastTacticIndex          = 0;

    public TacticalSystem(PathfinderMob self) {
        this.self = self;
        for (int i = 0; i < tacticScores.length; i++) tacticScores[i] = 0.5f;
    }

    public void executeTactics(float[] neuralOutput, ServerPlayer target, PlayerIntelligence intel) {
        tacticLockTicks  = Math.max(0, tacticLockTicks - 1);
        dashCooldown     = Math.max(0, dashCooldown - 1);
        directionChangeCooldown = Math.max(0, directionChangeCooldown - 1);

        double dist        = self.distanceTo(target);
        float  myHPRatio   = self.getHealth() / self.getMaxHealth();
        float  tgHP        = target.getHealth();
        float  tgMaxHP     = target.getMaxHealth();
        boolean playerFleeing = intel.predictingFlee(tgHP, tgMaxHP)
            || (tgHP / tgMaxHP < intel.fleeHealthThreshold + 0.08f);

        if (self.isInWater()) { setTactic(ActiveTactic.SWIMMING); }
        else if (myHPRatio < 0.18f) { setTactic(ActiveTactic.RETREAT); }
        else if (isAmbushing) {   }
        else if (tacticLockTicks > 0) {   }
        else {
            chooseTactic(dist, myHPRatio, playerFleeing, target, intel);
        }

        executeCurrent(target, intel, dist, playerFleeing);
    }

    private void chooseTactic(double dist, float myHPRatio, boolean playerFleeing,
                               ServerPlayer target, PlayerIntelligence intel) {
        
        if (playerFleeing && intel.tendsToFlee) {
            setTactic(ActiveTactic.PURSUIT);
            return;
        }

        switch (intel.bestStrategyAgainstThem) {
            case RANGED_KITE -> {
                
                if (dist > 8) setTactic(ActiveTactic.INTERCEPT);
                else           setTactic(ActiveTactic.FLANKING);
                return;
            }
            case AGGRESSIVE_MELEE -> {
                
                if (dist < 6) setTactic(ActiveTactic.FLANKING);
                else           setTactic(ActiveTactic.AMBUSH_SETUP);
                return;
            }
            case PURSUIT -> {
                
                setTactic(ActiveTactic.AMBUSH_SETUP);
                return;
            }
            case AGGRESSIVE_FLANKING -> {
                
                setTactic(ActiveTactic.INTERCEPT);
                return;
            }
        }

        if (dist > 100 && intel.miningHotspot != null) {
            
            setTactic(ActiveTactic.MINING_AMBUSH);
        } else if (dist > 50) {
            setTactic(ActiveTactic.STALKING);
        } else if (dist > 12 && intel.dataConfidence > 30) {
            setTactic(ActiveTactic.AMBUSH_SETUP);
        } else if (dist > 4) {
            setTactic(ActiveTactic.AGGRESSIVE);
        } else {
            setTactic(ActiveTactic.FLANKING);
        }
    }

    private void executeCurrent(ServerPlayer target, PlayerIntelligence intel,
                                 double dist, boolean playerFleeing) {
        switch (currentTactic) {
            case SWIMMING        -> executeSwimming(target);
            case RETREAT         -> executeRetreat(target);
            case STALKING        -> executeStalk(target, intel);
            case AMBUSH_SETUP    -> executeAmbushSetup(target, intel);
            case AMBUSH_STRIKE   -> executeAmbushStrike(target);
            case AGGRESSIVE      -> executeAggressive(target, intel);
            case FLANKING        -> executeFlanking(target, intel);
            case INTERCEPT       -> executeIntercept(target, intel);
            case PURSUIT         -> executePursuit(target, intel);
            case DEATHSPOT_WAIT  -> executeDeathspotWait(target, intel);
            case MINING_AMBUSH   -> executeMiningAmbush(target, intel);
            case EVASION         -> executeEvasion(target);
        }
    }

    private void executeSwimming(ServerPlayer target) {
        Vec3 dir = target.position().subtract(self.position()).normalize();
        self.getNavigation().moveTo(
            self.getX() + dir.x * 3, self.getY() + dir.y * 2, self.getZ() + dir.z * 3, 1.7);
    }

    private void executeRetreat(ServerPlayer target) {
        Vec3 away = self.position().subtract(target.position()).normalize();
        self.getNavigation().moveTo(
            self.getX() + away.x * 40, self.getY(), self.getZ() + away.z * 40, 1.9);
    }

    private void executeStalk(ServerPlayer target, PlayerIntelligence intel) {
        stalkAngle += 0.015f * (intel.tendsToFlee ? 0.7f : 1.2f);
        double dist    = self.distanceTo(target);
        double radius  = intel.preferredEngageRange * 3.0 + 10.0;

        if (dist > 80) {
            
            self.getNavigation().moveTo(target, 1.15);
            return;
        }

        Vec3 tp = target.position();
        Vec3 orbitTarget = new Vec3(
            tp.x + Math.sin(stalkAngle) * radius,
            tp.y,
            tp.z + Math.cos(stalkAngle) * radius
        );

        if (intel.caution > 0.7f && dist < 100) {
            self.getNavigation().moveTo(orbitTarget.x, orbitTarget.y, orbitTarget.z, 0.95);
        } else {
            self.getNavigation().moveTo(orbitTarget.x, orbitTarget.y, orbitTarget.z, 1.1);
        }
    }

    private void executeAmbushSetup(ServerPlayer target, PlayerIntelligence intel) {
        if (isAmbushing) {
            continueAmbush(target);
            return;
        }

        Vec3 ambushVec = Vec3.atCenterOf(
            intel.getBestAmbushPosition(self.position(), target.position(), target.getDeltaMovement()));

        ambushPos    = BlockPos.containing(ambushVec);
        isAmbushing  = true;
        
        ambushWaitTicks = 60 + (int)(intel.caution * 120);
        setTacticLock(ambushWaitTicks + 40);
    }

    private void continueAmbush(ServerPlayer target) {
        if (ambushPos == null) { isAmbushing = false; return; }

        double distToPos = self.distanceToSqr(ambushPos.getX(), ambushPos.getY(), ambushPos.getZ());

        if (distToPos > 9) {
            self.getNavigation().moveTo(ambushPos.getX(), ambushPos.getY(), ambushPos.getZ(), 1.25);
        } else {
            ambushWaitTicks--;
            self.getNavigation().stop();
            
            Vec3 toPlayer = target.position().subtract(self.position());
            if (toPlayer.length() > 0.1) {
                float yaw = (float)(Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z)));
                self.setYRot(yaw);
                self.yHeadRot = yaw;
            }
            if (ambushWaitTicks <= 0) {
                isAmbushing = false;
                setTactic(ActiveTactic.AMBUSH_STRIKE);
            }
        }
    }

    private void executeAmbushStrike(ServerPlayer target) {
        self.setTarget(target);
        if (dashCooldown == 0) {
            Vec3 dash = target.position().subtract(self.position()).normalize().scale(1.4);
            self.setDeltaMovement(dash.x, 0.3, dash.z);
            dashCooldown = 30;
        }
        self.getNavigation().moveTo(target, 1.8);
        if (self.distanceTo(target) < 3) setTactic(ActiveTactic.AGGRESSIVE);
    }

    private void executeAggressive(ServerPlayer target, PlayerIntelligence intel) {
        self.setTarget(target);

        if (intel.sprintAttacks && self.distanceTo(target) < 4 && dashCooldown == 0) {
            Vec3 side = new Vec3(-target.getDeltaMovement().z, 0, target.getDeltaMovement().x)
                .normalize().scale(2.5 * circleDirection);
            self.setDeltaMovement(side.x, self.getDeltaMovement().y, side.z);
            dashCooldown = 20;
            if (directionChangeCooldown == 0) {
                circleDirection *= -1;
                directionChangeCooldown = 40 + rng.nextInt(40);
            }
        }

        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(6));
        self.getNavigation().moveTo(predicted.x, predicted.y, predicted.z, 1.5);
    }

    private void executeFlanking(ServerPlayer target, PlayerIntelligence intel) {
        
        Vec3 playerFwd = target.getLookAngle();
        Vec3 myFlankDir = new Vec3(-playerFwd.z, 0, playerFwd.x).normalize();

        if (intel.circleStrafes) {
            myFlankDir = myFlankDir.scale(-circleDirection);
        }

        double dist = self.distanceTo(target);
        Vec3 predictedTarget = target.position().add(target.getDeltaMovement().scale(12));
        Vec3 flankPos = predictedTarget.add(myFlankDir.scale(3 + dist * 0.3));

        self.setTarget(target);
        self.getNavigation().moveTo(flankPos.x, flankPos.y, flankPos.z, 1.4);
    }

    private void executeIntercept(ServerPlayer target, PlayerIntelligence intel) {
        
        Vec3 dest = Vec3.atCenterOf(
            intel.predictDestination(target.position(), target.getDeltaMovement()));
        double myDist = self.position().distanceTo(dest);
        double theirDist = target.position().distanceTo(dest);

        if (myDist < theirDist * 1.2) {
            self.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.6);
        } else {
            
            Vec3 shortcut = self.position().lerp(dest, 0.3);
            self.getNavigation().moveTo(shortcut.x, shortcut.y, shortcut.z, 1.7);
        }
        self.setTarget(target);
    }

    private void executePursuit(ServerPlayer target, PlayerIntelligence intel) {
        
        BlockPos fleeTarget = intel.homePos != null ? intel.homePos : null;
        Vec3 predicted = Vec3.atCenterOf(
            intel.predictDestination(target.position(), target.getDeltaMovement()));

        if (fleeTarget != null && intel.dataConfidence > 50) {
            
            Vec3 toHome = Vec3.atCenterOf(fleeTarget).subtract(target.position()).normalize();
            Vec3 cutoff = target.position().add(toHome.scale(15));
            self.getNavigation().moveTo(cutoff.x, cutoff.y, cutoff.z, 1.75);
        } else {
            self.getNavigation().moveTo(predicted.x, predicted.y, predicted.z, 1.7);
        }
        self.setTarget(target);
    }

    private void executeDeathspotWait(ServerPlayer target, PlayerIntelligence intel) {
        BlockPos spot = intel.getMostRecentDeathSpot();
        if (spot == null) { setTactic(ActiveTactic.STALKING); return; }

        double distToSpot = self.distanceToSqr(spot.getX(), spot.getY(), spot.getZ());
        if (distToSpot > 16) {
            self.getNavigation().moveTo(spot.getX(), spot.getY(), spot.getZ(), 1.2);
        } else {
            
            self.getNavigation().stop();
            Vec3 toPlayer = target.position().subtract(self.position());
            if (toPlayer.length() > 0.1) {
                float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
                self.setYRot(yaw);
                self.yHeadRot = yaw;
            }
            if (self.distanceTo(target) < 12) {
                setTactic(ActiveTactic.AGGRESSIVE);
            }
        }
    }

    private void executeMiningAmbush(ServerPlayer target, PlayerIntelligence intel) {
        if (intel.miningHotspot == null) { setTactic(ActiveTactic.STALKING); return; }

        BlockPos minePos = new BlockPos(
            intel.miningHotspot.getX(),
            intel.preferredMiningY,
            intel.miningHotspot.getZ()
        );

        double distToMine = self.distanceToSqr(minePos.getX(), minePos.getY(), minePos.getZ());

        if (distToMine > 25) {
            self.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(), 1.2);
        } else {
            
            self.getNavigation().stop();
            if (self.distanceTo(target) < 20) setTactic(ActiveTactic.AMBUSH_STRIKE);
        }
    }

    private void executeEvasion(ServerPlayer target) {
        Vec3 away = self.position().subtract(target.position()).normalize();
        Vec3 perp = new Vec3(-away.z, 0, away.x).scale(rng.nextBoolean() ? 4 : -4);
        Vec3 evadeTarget = self.position().add(away.scale(12)).add(perp);
        self.getNavigation().moveTo(evadeTarget.x, evadeTarget.y, evadeTarget.z, 1.7);
    }

    private void setTactic(ActiveTactic tactic) {
        if (currentTactic != tactic) {
            lastTacticIndex = tactic.ordinal();
            currentTactic   = tactic;
            isAmbushing     = (tactic == ActiveTactic.AMBUSH_SETUP);
        }
    }

    private void setTacticLock(int ticks) {
        tacticLockTicks = ticks;
    }

    public void onHitTarget() {
        consecutiveHits++;
        consecutiveMisses = 0;
        tacticScores[lastTacticIndex] = Math.min(1f, tacticScores[lastTacticIndex] + 0.1f);
    }

    public void onMissTarget() {
        consecutiveMisses++;
        consecutiveHits = 0;
        tacticScores[lastTacticIndex] = Math.max(0f, tacticScores[lastTacticIndex] - 0.05f);
        
        if (consecutiveMisses >= 3) {
            setTactic(ActiveTactic.FLANKING);
            consecutiveMisses = 0;
        }
    }

    public ActiveTactic getCurrentTactic()  { return currentTactic; }
    public boolean isAmbushing()            { return isAmbushing; }
    public int getConsecutiveHits()         { return consecutiveHits; }
    public int getConsecutiveMisses()       { return consecutiveMisses; }
    public float getTacticScore(ActiveTactic t) { return tacticScores[t.ordinal()]; }
}
