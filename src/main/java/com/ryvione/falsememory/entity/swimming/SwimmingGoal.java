package com.ryvione.falsememory.entity.swimming;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

public class SwimmingGoal extends Goal {

    private PathfinderMob mob;
    private double targetX;
    private double targetY;
    private double targetZ;

    public SwimmingGoal(PathfinderMob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return mob.isInWater();
    }

    @Override
    public void tick() {
        if (!mob.isInWater()) return;

        if (mob.getTarget() != null) {
            targetX = mob.getTarget().getX();
            targetY = mob.getTarget().getY();
            targetZ = mob.getTarget().getZ();

            swim(targetX, targetY, targetZ);
        }
    }

    private void swim(double x, double y, double z) {
        double dx = x - mob.getX();
        double dy = y - mob.getY();
        double dz = z - mob.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.1) {
            dx /= dist;
            dz /= dist;

            mob.setDeltaMovement(
                dx * 0.4,
                Math.min(0.3, dy * 0.1),
                dz * 0.4
            );
        }

        mob.getNavigation().moveTo(x, y, z, 1.2);
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isInWater() && mob.getTarget() != null;
    }

    @Override
    public void stop() {
    }
}
