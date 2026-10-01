package dev.bettermoving.physics;

import static dev.bettermoving.config.FluidMovementModel.AIR;
import static dev.bettermoving.config.FluidMovementModel.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class BetterMovingMovementPolicyTest {
    @Test
    void airModelTreatsEveryDetectedFluidAsAirForTheLocalPlayer() {
        assertFalse(BetterMovingMovementPolicy.resolveWaterMovementState(true, AIR, true, true, false));
        assertFalse(BetterMovingMovementPolicy.resolveWaterMovementState(true, AIR, true, false, true));
    }

    @Test
    void waterModelRoutesWaterAndOtherFluidsThroughWaterMovement() {
        assertTrue(BetterMovingMovementPolicy.resolveWaterMovementState(true, WATER, true, true, false));
        assertTrue(BetterMovingMovementPolicy.resolveWaterMovementState(true, WATER, true, false, true));
    }

    @Test
    void disabledFeaturePreservesVanillaDetectionForEitherModel() {
        assertTrue(BetterMovingMovementPolicy.resolveWaterMovementState(false, AIR, true, true, false));
        assertFalse(BetterMovingMovementPolicy.resolveWaterMovementState(false, WATER, true, false, true));
    }

    @Test
    void enabledFeaturePreservesOtherEntitiesVanillaDetection() {
        assertTrue(BetterMovingMovementPolicy.resolveWaterMovementState(true, AIR, false, true, false));
        assertFalse(BetterMovingMovementPolicy.resolveWaterMovementState(true, WATER, false, false, true));
    }

    @Test
    void waterMovementEffectsFollowTheSelectedModel() {
        assertFalse(BetterMovingMovementPolicy.shouldApplyWaterMovement(true, AIR, true));
        assertTrue(BetterMovingMovementPolicy.shouldApplyWaterMovement(true, WATER, true));
        assertTrue(BetterMovingMovementPolicy.shouldApplyWaterMovement(false, AIR, true));
        assertTrue(BetterMovingMovementPolicy.shouldApplyWaterMovement(true, AIR, false));
    }

    @Test
    void bothModelsSuppressBubbleColumnLiftAndDrag() {
        assertFalse(BetterMovingMovementPolicy.shouldApplyBubbleColumnMovement(true, true));
        assertTrue(BetterMovingMovementPolicy.shouldApplyBubbleColumnMovement(false, true));
        assertTrue(BetterMovingMovementPolicy.shouldApplyBubbleColumnMovement(true, false));
    }

    @Test
    void waterModelUsesWaterCurrentSpeedForLava() {
        assertEquals(
                BetterMovingMovementPolicy.WATER_CURRENT_SPEED,
                BetterMovingMovementPolicy.resolveLavaCurrentSpeed(true, WATER, true, 0.0023333333333333335));
        assertEquals(0.007, BetterMovingMovementPolicy.resolveLavaCurrentSpeed(false, WATER, true, 0.007));
        assertEquals(0.007, BetterMovingMovementPolicy.resolveLavaCurrentSpeed(true, AIR, true, 0.007));
    }

    @Test
    void activeModelsRemoveTheLavaFallMultiplierAndWaterModelResetsFallDistance() {
        assertEquals(1.0f, BetterMovingMovementPolicy.resolveLavaFallDistanceMultiplier(true, AIR, true, 0.5f));
        assertEquals(1.0f, BetterMovingMovementPolicy.resolveLavaFallDistanceMultiplier(true, WATER, true, 0.5f));
        assertEquals(0.5f, BetterMovingMovementPolicy.resolveLavaFallDistanceMultiplier(false, WATER, true, 0.5f));
        assertFalse(BetterMovingMovementPolicy.shouldResetLavaFallDistanceAsWater(true, AIR, true));
        assertTrue(BetterMovingMovementPolicy.shouldResetLavaFallDistanceAsWater(true, WATER, true));
    }

    @Test
    void riptideEligibilityFollowsTheSelectedModel() {
        assertFalse(BetterMovingMovementPolicy.resolveRiptideEnvironment(true, AIR, true, true, false, false));
        assertTrue(BetterMovingMovementPolicy.resolveRiptideEnvironment(true, AIR, true, true, true, false));
        assertTrue(BetterMovingMovementPolicy.resolveRiptideEnvironment(true, WATER, true, false, false, true));
        assertFalse(BetterMovingMovementPolicy.resolveRiptideEnvironment(false, WATER, true, false, false, true));
        assertTrue(BetterMovingMovementPolicy.resolveRiptideEnvironment(false, WATER, true, true, false, false));
    }
}
