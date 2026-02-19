package com.ryvione.falsememory.ai.perception;

import com.ryvione.falsememory.ai.intel.PlayerIntelligence;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public class PerceptionSystem {

    public static final int INPUT_SIZE = 48;

    private static final int HISTORY_LEN = 24;
    private final float[][] posHistory = new float[HISTORY_LEN][2];
    private int histIdx = 0;

    public PerceptionSystem(ServerPlayer player) {}

    public float[] perceive(LivingEntity self, ServerPlayer target, PlayerIntelligence intel) {
        float[] in = new float[INPUT_SIZE];

        double dist = self.distanceTo(target);
        in[0] = clamp(dist / 256.0);
        in[1] = self.getHealth() / self.getMaxHealth();
        in[2] = target.getHealth() / target.getMaxHealth();

        Vec3 myVel = self.getDeltaMovement();
        Vec3 tgVel = target.getDeltaMovement();
        in[3] = clamp(mag2D(myVel) / 0.5);
        in[4] = clamp(mag2D(tgVel) / 0.5);

        Vec3 dir = target.position().subtract(self.position());
        double dLen = dir.length();
        if (dLen > 0.001) dir = dir.normalize();
        in[5] = (float) dir.x;
        in[6] = (float) dir.z;
        in[7] = (float) dir.y;

        in[8]  = self.isInWater() ? 1f : 0f;
        in[9]  = target.isInWater() ? 1f : 0f;
        in[10] = self.onGround() ? 1f : 0f;
        in[11] = target.onGround() ? 1f : 0f;
        in[12] = self.getHealth()   < self.getMaxHealth()   * 0.3f ? 1f : 0f;
        in[13] = target.getHealth() < target.getMaxHealth() * 0.3f ? 1f : 0f;

        float yawDiff = target.getYRot() - self.getYRot();
        while (yawDiff >  180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        in[14] = (float) Math.sin(Math.toRadians(yawDiff));
        in[15] = (float) Math.cos(Math.toRadians(yawDiff));

        in[16] = self.isBlocking()   ? 1f : 0f;
        in[17] = target.isBlocking() ? 1f : 0f;

        float heightDiff = (float)((target.getY() - self.getY()) / 2.0);
        in[18] = Math.max(-1f, Math.min(1f, heightDiff));
        in[19] = self.isOnFire()      ? 1f : 0f;
        in[20] = target.isSprinting() ? 1f : 0f;
        in[21] = target.isCrouching() ? 1f : 0f;

        ItemStack mainHand = target.getMainHandItem();
        in[22] = mainHand.getItem() instanceof SwordItem    ? 1f : 0f;
        in[23] = mainHand.getItem() instanceof AxeItem      ? 1f : 0f;
        in[24] = mainHand.getItem() instanceof BowItem      ? 1f : 0f;
        in[25] = mainHand.getItem() instanceof CrossbowItem ? 1f : 0f;
        in[26] = target.getOffhandItem().getItem() instanceof ShieldItem ? 1f : 0f;

        int armorPieces = 0;
        for (var armor : target.getArmorSlots()) if (!armor.isEmpty()) armorPieces++;
        in[27] = armorPieces / 4f;

        Vec3 myLook = self.getLookAngle();
        Vec3 tgLook = target.getLookAngle();
        in[28] = (float) myLook.dot(tgLook);

        Vec3 predicted = target.position().add(tgVel.scale(10));
        Vec3 toPred    = predicted.subtract(self.position());
        double tpLen   = toPred.length();
        if (tpLen > 0.001) toPred = toPred.normalize();
        in[29] = (float) toPred.x;
        in[30] = (float) toPred.z;

        int prevIdx = (histIdx - 1 + HISTORY_LEN) % HISTORY_LEN;
        float dvx = (float)(tgVel.x - posHistory[prevIdx][0]);
        float dvz = (float)(tgVel.z - posHistory[prevIdx][1]);
        in[31] = clamp(Math.sqrt(dvx * dvx + dvz * dvz) * 5.0);

        in[32] = intel.fleeHealthThreshold;
        in[33] = intel.boldness;
        in[34] = intel.caution;
        in[35] = intel.tendsToFlee ? 1f : 0f;
        in[36] = intel.sprintAttacks ? 1f : 0f;
        in[37] = intel.circleStrafes ? 1f : 0f;
        in[38] = intel.usesPotions   ? 1f : 0f;
        in[39] = intel.panicsAtLowHP ? 1f : 0f;

        float hpRatio = target.getHealth() / target.getMaxHealth();
        in[40] = intel.predictingFlee(target.getHealth(), target.getMaxHealth()) ? 1f : 0f;

        if (intel.homePos != null) {
            double homeDistTarget = target.blockPosition().distSqr(intel.homePos);
            in[41] = clamp(Math.sqrt(homeDistTarget) / 200.0);
        }

        if (!intel.deathSpots.isEmpty()) {
            BlockPos lastDeath = intel.deathSpots.get(intel.deathSpots.size() - 1);
            in[42] = target.blockPosition().closerThan(lastDeath, 16) ? 1f : 0f;
        }

        in[43] = intel.adaptability;
        in[44] = intel.dataConfidence / 100f;
        in[45] = intel.estimatedArmorTiers / 4f;

        in[46] = intel.bestStrategyAgainstThem.ordinal() / 4f;

        boolean currentPanic = hpRatio < 0.25f && mag2D(tgVel) > 0.3f;
        in[47] = currentPanic ? 1f : 0f;

        recordHistory(tgVel);
        return in;
    }

    private void recordHistory(Vec3 vel) {
        posHistory[histIdx][0] = (float) vel.x;
        posHistory[histIdx][1] = (float) vel.z;
        histIdx = (histIdx + 1) % HISTORY_LEN;
    }

    private float mag2D(Vec3 v) {
        return (float) Math.sqrt(v.x * v.x + v.z * v.z);
    }

    private float clamp(double v) {
        return (float) Math.min(1.0, Math.max(0.0, v));
    }
}
