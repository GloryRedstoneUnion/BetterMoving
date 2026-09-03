package dev.fluidair.physics;

import dev.fluidair.config.FluidMovementModel;

public final class FluidAirMovementPolicy {
    public static final double WATER_CURRENT_SPEED = 0.014;

    private FluidAirMovementPolicy() {
    }

    public static boolean resolveWaterMovementState(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer,
            boolean detectedWaterState,
            boolean detectedOtherFluidState) {
        if (!isActive(enabled, localPlayer)) {
            return detectedWaterState;
        }
        return model == FluidMovementModel.WATER
                && (detectedWaterState || detectedOtherFluidState);
    }

    public static boolean shouldApplyWaterMovement(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer) {
        return !isActive(enabled, localPlayer) || model == FluidMovementModel.WATER;
    }

    public static boolean shouldApplyBubbleColumnMovement(
            boolean enabled,
            boolean localPlayer) {
        return !isActive(enabled, localPlayer);
    }

    public static double resolveLavaCurrentSpeed(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer,
            double vanillaSpeed) {
        return isActive(enabled, localPlayer) && model == FluidMovementModel.WATER
                ? WATER_CURRENT_SPEED
                : vanillaSpeed;
    }

    public static boolean shouldResetLavaFallDistanceAsWater(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer) {
        return isActive(enabled, localPlayer) && model == FluidMovementModel.WATER;
    }

    public static float resolveLavaFallDistanceMultiplier(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer,
            float vanillaMultiplier) {
        return isActive(enabled, localPlayer) ? 1.0f : vanillaMultiplier;
    }

    public static boolean resolveRiptideEnvironment(
            boolean enabled,
            FluidMovementModel model,
            boolean localPlayer,
            boolean touchingWaterOrRain,
            boolean rainingAtPlayer,
            boolean touchingOtherFluid) {
        if (!isActive(enabled, localPlayer)) {
            return touchingWaterOrRain;
        }
        return model == FluidMovementModel.WATER
                ? touchingWaterOrRain || touchingOtherFluid
                : rainingAtPlayer;
    }

    private static boolean isActive(boolean enabled, boolean localPlayer) {
        return enabled && localPlayer;
    }
}
