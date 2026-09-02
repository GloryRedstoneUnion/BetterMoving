package dev.fluidair.physics;

import static dev.fluidair.config.FluidMovementModel.AIR;
import static dev.fluidair.config.FluidMovementModel.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FluidAirMovementPolicyTest {
    @Test
    void airModelTreatsEveryDetectedFluidAsAirForTheLocalPlayer() {
        assertFalse(FluidAirMovementPolicy.resolveWaterMovementState(true, AIR, true, true, false));
        assertFalse(FluidAirMovementPolicy.resolveWaterMovementState(true, AIR, true, false, true));
        assertFalse(FluidAirMovementPolicy.resolveLavaMovementState(true, AIR, true, true));
        assertFalse(FluidAirMovementPolicy.resolveLavaFluidHeightState(true, AIR, true, true));
    }

    @Test
    void waterModelRoutesWaterAndOtherFluidsThroughWaterMovement() {
        assertTrue(FluidAirMovementPolicy.resolveWaterMovementState(true, WATER, true, true, false));
        assertTrue(FluidAirMovementPolicy.resolveWaterMovementState(true, WATER, true, false, true));
        assertFalse(FluidAirMovementPolicy.resolveLavaMovementState(true, WATER, true, true));
        assertTrue(FluidAirMovementPolicy.resolveLavaFluidHeightState(true, WATER, true, true));
    }

    @Test
    void disabledFeaturePreservesVanillaDetectionForEitherModel() {
        assertTrue(FluidAirMovementPolicy.resolveWaterMovementState(false, AIR, true, true, false));
        assertFalse(FluidAirMovementPolicy.resolveWaterMovementState(false, WATER, true, false, true));
        assertTrue(FluidAirMovementPolicy.resolveLavaMovementState(false, WATER, true, true));
        assertTrue(FluidAirMovementPolicy.resolveLavaFluidHeightState(false, AIR, true, true));
    }

    @Test
    void enabledFeaturePreservesOtherEntitiesVanillaDetection() {
        assertTrue(FluidAirMovementPolicy.resolveWaterMovementState(true, AIR, false, true, false));
        assertFalse(FluidAirMovementPolicy.resolveWaterMovementState(true, WATER, false, false, true));
        assertTrue(FluidAirMovementPolicy.resolveLavaMovementState(true, WATER, false, true));
    }

    @Test
    void waterMovementEffectsFollowTheSelectedModel() {
        assertFalse(FluidAirMovementPolicy.shouldApplyWaterMovement(true, AIR, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyWaterMovement(true, WATER, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyWaterMovement(false, AIR, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyWaterMovement(true, AIR, false));
    }

    @Test
    void bothModelsSuppressBubbleColumnLiftAndDrag() {
        assertFalse(FluidAirMovementPolicy.shouldApplyBubbleColumnMovement(true, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyBubbleColumnMovement(false, true));
        assertTrue(FluidAirMovementPolicy.shouldApplyBubbleColumnMovement(true, false));
    }

    @Test
    void waterModelUsesWaterCurrentSpeedForLava() {
        assertEquals(
                FluidAirMovementPolicy.WATER_CURRENT_SPEED,
                FluidAirMovementPolicy.resolveLavaCurrentSpeed(true, WATER, true, 0.0023333333333333335));
        assertEquals(0.007, FluidAirMovementPolicy.resolveLavaCurrentSpeed(false, WATER, true, 0.007));
        assertEquals(0.007, FluidAirMovementPolicy.resolveLavaCurrentSpeed(true, AIR, true, 0.007));
    }

    @Test
    void activeModelsRemoveTheLavaFallMultiplierAndWaterModelResetsFallDistance() {
        assertEquals(1.0f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(true, AIR, true, 0.5f));
        assertEquals(1.0f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(true, WATER, true, 0.5f));
        assertEquals(0.5f, FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(false, WATER, true, 0.5f));
        assertFalse(FluidAirMovementPolicy.shouldResetLavaFallDistanceAsWater(true, AIR, true));
        assertTrue(FluidAirMovementPolicy.shouldResetLavaFallDistanceAsWater(true, WATER, true));
    }

    @Test
    void riptideEligibilityFollowsTheSelectedModel() {
        assertFalse(FluidAirMovementPolicy.resolveRiptideEnvironment(true, AIR, true, true, false, false));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(true, AIR, true, true, true, false));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(true, WATER, true, false, false, true));
        assertFalse(FluidAirMovementPolicy.resolveRiptideEnvironment(false, WATER, true, false, false, true));
        assertTrue(FluidAirMovementPolicy.resolveRiptideEnvironment(false, WATER, true, true, false, false));
    }
}
