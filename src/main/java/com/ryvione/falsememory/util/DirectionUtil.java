package com.ryvione.falsememory.util;

public class DirectionUtil {

    public static String getDirectionName(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 22.5 || yaw >= 337.5) return "North";
        if (yaw < 67.5) return "Northeast";
        if (yaw < 112.5) return "East";
        if (yaw < 157.5) return "Southeast";
        if (yaw < 202.5) return "South";
        if (yaw < 247.5) return "Southwest";
        if (yaw < 292.5) return "West";
        return "Northwest";
    }

    public static float yawFromBucket(int bucket) {
        return bucket * 45f;
    }

    public static int bucketFromYaw(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        return (int)(normalized / 45f) % 8;
    }
}