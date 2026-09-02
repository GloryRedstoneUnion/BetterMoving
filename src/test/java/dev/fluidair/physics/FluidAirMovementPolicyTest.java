package dev.fluidair.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FluidAirMovementPolicyTest {
    @Test
    void enabledModeTreatsDetectedFluidAsAirForTheLocalPlayer() {
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementState(true, true, true));
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementState(true, true, false));
    }

    @Test
    void disabledModePreservesTheLocalPlayersDetectedFluidState() {
        assertTrue(FluidAirMovementPolicy.resolveFluidMovementState(false, true, true));
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementState(false, true, false));
    }

    @Test
    void enabledModePreservesOtherEntitiesDetectedFluidState() {
        assertTrue(FluidAirMovementPolicy.resolveFluidMovementState(true, false, true));
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementState(true, false, false));
    }

    @Test
    void enabledModeSkipsFluidMovementForTheLocalPlayerOnly() {
        assertFalse(FluidAirMovementPolicy.shouldApplyFluidMovement(true, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyFluidMovement(false, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyFluidMovement(true, false));
    }

    @Test
    void enabledModePreservesFallDistanceInLavaForTheLocalPlayerOnly() {
        assertEquals(1.0f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(true, true, 0.5f));
        assertEquals(0.5f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(false, true, 0.5f));
        assertEquals(0.5f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(true, false, 0.5f));
    }

    @Test
    void enabledModeAllowsRiptideForRainButNotWater() {
        assertFalse(FluidAirMovementPolicy.resolveRiptideEnvironment(true, true, true, false));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(true, true, true, true));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(false, true, true, false));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(true, false, true, false));
    }
}
