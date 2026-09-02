package dev.fluidair.physics;

public final class FluidAirMovementPolicy {
    private FluidAirMovementPolicy() {
    }

    public static boolean resolveFluidMovementState(
            boolean ignoreFluidPhysics,
            boolean localPlayer,
            boolean detectedFluidState) {
        return shouldApplyFluidMovement(ignoreFluidPhysics, localPlayer)
                && detectedFluidState;
    }

    public static boolean shouldApplyFluidMovement(
            boolean ignoreFluidPhysics,
            boolean localPlayer) {
        return !ignoreFluidPhysics || !localPlayer;
    }

    public static float resolveLavaFallDistanceMultiplier(
            boolean ignoreFluidPhysics,
            boolean localPlayer,
            float vanillaMultiplier) {
        return shouldApplyFluidMovement(ignoreFluidPhysics, localPlayer)
                ? vanillaMultiplier
                : 1.0f;
    }

    public static boolean resolveRiptideEnvironment(
            boolean ignoreFluidPhysics,
            boolean localPlayer,
            boolean touchingWaterOrRain,
            boolean rainingAtPlayer) {
        return shouldApplyFluidMovement(ignoreFluidPhysics, localPlayer)
                ? touchingWaterOrRain
                : rainingAtPlayer;
    }
}
