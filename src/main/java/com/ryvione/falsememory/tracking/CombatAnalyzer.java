package com.ryvione.falsememory.tracking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.*;


public class CombatAnalyzer {

    private final ServerPlayer player;
    private final LinkedList<CombatSnapshot> recentSnapshots = new LinkedList<>();
    private static final int SNAPSHOT_WINDOW = 40; 

    private int sprintAttackCount = 0;
    private int standsGroundCount = 0;
    private int circleStrafesCount = 0;
    private int weaponSwitchesCount = 0;
    private int potionUsesCount = 0;
    private int blockBreaksCount = 0;
    private int panicsAtLowHealthCount = 0;

    private double avgDistanceToTarget = 5.0;
    private double maxDistanceReached = 0.0;
    private double minDistanceReached = Double.MAX_VALUE;

    private Vec3 lastKnownPosition = null;
    private double totalDistanceTraveled = 0.0;

    public CombatAnalyzer(ServerPlayer player) {
        this.player = player;
    }

    public void recordCombatTick(Entity target) {
        CombatSnapshot snap = new CombatSnapshot(
            player.position(),
            player.getMainHandItem().copy(),
            player.getHealth(),
            player.isSprinting(),
            player.isShiftKeyDown(),
            (target != null) ? player.distanceTo(target) : 0.0,
            player.getAbilities().flying
        );

        recentSnapshots.add(snap);
        if (recentSnapshots.size() > SNAPSHOT_WINDOW) {
            recentSnapshots.removeFirst();
        }

        if (lastKnownPosition != null) {
            totalDistanceTraveled += lastKnownPosition.distanceTo(snap.position);
        }
        lastKnownPosition = snap.position;

        avgDistanceToTarget = snap.distanceToTarget;
        maxDistanceReached = Math.max(maxDistanceReached, snap.distanceToTarget);
        minDistanceReached = Math.min(minDistanceReached, snap.distanceToTarget);

        analyzeCurrentPattern();
    }


    private void analyzeCurrentPattern() {
        if (recentSnapshots.size() < 3) return;

        CombatSnapshot current = recentSnapshots.getLast();
        CombatSnapshot previous = recentSnapshots.get(recentSnapshots.size() - 2);

        if (current.isSprinting && current.position.distanceTo(previous.position) > 0.5) {
            sprintAttackCount++;
        }

        if (!current.isSprinting && current.position.distanceTo(previous.position) < 0.2) {
            standsGroundCount++;
        }

        if (isCircleStrafing()) {
            circleStrafesCount++;
        }

        if (!ItemStack.isSameItemSameComponents(current.mainhandItem, previous.mainhandItem)) {
            weaponSwitchesCount++;
        }

        if (current.health < 8 && current.isSprinting) {
            panicsAtLowHealthCount++;
        }

        if (!current.position.equals(previous.position)) {
            blockBreaksCount++;
        }
    }


    private boolean isCircleStrafing() {
        if (recentSnapshots.size() < 5) return false;

        CombatSnapshot current = recentSnapshots.getLast();
        CombatSnapshot old = recentSnapshots.get(recentSnapshots.size() - 5);

        Vec3 movement = current.position.subtract(old.position);
        if (movement.length() < 0.5) return false;

        return Math.abs(movement.x) > 0.2 && Math.abs(movement.z) > 0.2;
    }

    public String getSuggestedWeapon() {
        if (weaponSwitchesCount > 3) return "bow";

        if (standsGroundCount > sprintAttackCount) return "axe";

        return "sword";
    }

   
    public CombatStrategy getSuggestedStrategy() {
        if (sprintAttackCount > 5) {
            return CombatStrategy.RANGED_KITE;
        }

        if (circleStrafesCount > 4) {
            return CombatStrategy.AGGRESSIVE_FLANKING;
        }

        if (standsGroundCount > 6) {
            return CombatStrategy.AGGRESSIVE_MELEE;
        }

        if (panicsAtLowHealthCount > 3) {
            return CombatStrategy.PURSUIT;
        }

        return CombatStrategy.ADAPTIVE;
    }


    public Vec3 predictPlayerMovement(int ticksAhead) {
        if (recentSnapshots.size() < 3) return player.position();

        CombatSnapshot current = recentSnapshots.getLast();
        CombatSnapshot old = recentSnapshots.get(Math.max(0, recentSnapshots.size() - 10));

        Vec3 velocity = current.position.subtract(old.position).scale(1.0 / Math.max(1, recentSnapshots.size() - 10));
        return current.position.add(velocity.scale(ticksAhead));
    }


    public boolean isCurrentlyFleeing() {
        if (recentSnapshots.size() < 5) return false;

        CombatSnapshot current = recentSnapshots.getLast();
        return current.isSprinting && current.health < 10 && minDistanceReached < 3;
    }

    public double getLowestHealthReached() {
        return recentSnapshots.stream()
            .mapToDouble(s -> s.health)
            .min()
            .orElse(20.0);
    }


    public int getCombatDuration() {
        return recentSnapshots.size();
    }


    public CombatSnapshot getCurrentSnapshot() {
        return recentSnapshots.isEmpty() ? null : recentSnapshots.getLast();
    }

    public void reset() {
        recentSnapshots.clear();
        sprintAttackCount = 0;
        standsGroundCount = 0;
        circleStrafesCount = 0;
        weaponSwitchesCount = 0;
        potionUsesCount = 0;
        blockBreaksCount = 0;
        panicsAtLowHealthCount = 0;
        avgDistanceToTarget = 5.0;
        maxDistanceReached = 0.0;
        minDistanceReached = Double.MAX_VALUE;
        lastKnownPosition = null;
        totalDistanceTraveled = 0.0;
    }


    public int getSprintAttackCount() { return sprintAttackCount; }
    public int getStandsGroundCount() { return standsGroundCount; }
    public int getCircleStrafesCount() { return circleStrafesCount; }
    public int getWeaponSwitchesCount() { return weaponSwitchesCount; }
    public int getPanicCount() { return panicsAtLowHealthCount; }
    public double getTotalDistanceTraveled() { return totalDistanceTraveled; }
    public double getAverageDistance() { return avgDistanceToTarget; }
    public double getMaxDistanceReached() { return maxDistanceReached; }


    public static class CombatSnapshot {
        public final Vec3 position;
        public final net.minecraft.world.item.ItemStack mainhandItem;
        public final double health;
        public final boolean isSprinting;
        public final boolean isShifting;
        public final double distanceToTarget;
        public final boolean isFlying;

        public CombatSnapshot(Vec3 position, net.minecraft.world.item.ItemStack item, double health,
                            boolean sprinting, boolean shifting, double distToTarget, boolean flying) {
            this.position = position;
            this.mainhandItem = item;
            this.health = health;
            this.isSprinting = sprinting;
            this.isShifting = shifting;
            this.distanceToTarget = distToTarget;
            this.isFlying = flying;
        }
    }

    public enum CombatStrategy {
        AGGRESSIVE_MELEE,     
        RANGED_KITE,          
        AGGRESSIVE_FLANKING,   
        PURSUIT,               
        ADAPTIVE              
    }
}