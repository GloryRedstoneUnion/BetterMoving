package dev.fluidair.physics;

public final class FluidAirMovementPolicy {
    private FluidAirMovementPolicy() {
    }

    public static boolean resolveSwimmingPose(
            boolean ignoreFluidPhysics,
            boolean vanillaSwimmingPose) {
        return !ignoreFluidPhysics && vanillaSwimmingPose;
    }

    public static boolean useFluidJumpLogic(boolean ignoreFluidPhysics) {
        return !ignoreFluidPhysics;
    }

    public static boolean resolveFluidMovementCheck(
            boolean ignoreFluidPhysics,
            boolean detectedFluidState) {
        return !ignoreFluidPhysics && detectedFluidState;
    }
}
