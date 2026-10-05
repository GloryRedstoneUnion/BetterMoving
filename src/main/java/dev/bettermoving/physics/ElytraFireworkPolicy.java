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

    public static int customLifetime(
            boolean enabled,
            int flight,
            int vanillaLifetime,
            int flight1Lifetime,
            int flight2Lifetime,
            int flight3Lifetime) {
        if (!enabled) {
            return vanillaLifetime;
        }

        return switch (flight) {
            case 1 -> flight1Lifetime;
            case 2 -> flight2Lifetime;
            case 3 -> flight3Lifetime;
            default -> vanillaLifetime;
        };
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
