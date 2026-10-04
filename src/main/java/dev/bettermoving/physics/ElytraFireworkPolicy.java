package dev.bettermoving.physics;

import net.minecraft.util.math.Vec3d;

public final class ElytraFireworkPolicy {
    private ElytraFireworkPolicy() {
    }

    public static boolean shouldSimulate(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean holdingFirework) {
        return enabled && localPlayer && fallFlying && holdingFirework;
    }

    public static boolean shouldRedirectBlockUse(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean holdingFirework) {
        return enabled && localPlayer && fallFlying && holdingFirework;
    }

    public static boolean shouldBlockNonElytraBlockUse(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean elytraBlockUseEnabled,
            boolean holdingFirework) {
        return enabled
                && localPlayer
                && holdingFirework
                && (!fallFlying || !elytraBlockUseEnabled);
    }

    public static Vec3d applyTargetSpeed(
            Vec3d vanillaVelocity,
            Vec3d currentVelocity,
            Vec3d rotationVector,
            double targetSpeedMetersPerSecond,
            boolean enabled) {
        if (!enabled) {
            return vanillaVelocity;
        }

        double targetSpeedPerTick = targetSpeedMetersPerSecond / 20.0;
        return currentVelocity.multiply(0.5)
                .add(rotationVector.multiply(targetSpeedPerTick * 0.5));
    }
}
