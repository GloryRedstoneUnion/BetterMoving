package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

final class ElytraFireworkPolicyTest {
    @Test
    void customLifetimeOverridesOnlyFlightLevelsOneThroughThreeWhenEnabled() {
        assertEquals(17, ElytraFireworkPolicy.customLifetime(true, 1, 12, 17, 28, 39));
        assertEquals(28, ElytraFireworkPolicy.customLifetime(true, 2, 22, 17, 28, 39));
        assertEquals(39, ElytraFireworkPolicy.customLifetime(true, 3, 32, 17, 28, 39));
        assertEquals(8, ElytraFireworkPolicy.customLifetime(true, 0, 8, 17, 28, 39));
        assertEquals(48, ElytraFireworkPolicy.customLifetime(true, 4, 48, 17, 28, 39));
    }

    @Test
    void customLifetimePreservesVanillaWhenDisabled() {
        assertEquals(22, ElytraFireworkPolicy.customLifetime(false, 2, 22, 17, 28, 39));
    }

    @Test
    void simulationRequiresTheToggleAndAllFireworkConditions() {
        assertTrue(ElytraFireworkPolicy.shouldSimulate(true, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(false, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, false, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, true, false, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, true, true, false));
    }

    @Test
    void simulatedFireworkCancellationRequiresBothTogglesAndTheLocalPlayer() {
        assertTrue(ElytraFireworkPolicy.shouldCancelSimulatedFireworkOnGlideStop(true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldCancelSimulatedFireworkOnGlideStop(false, true, true));
        assertFalse(ElytraFireworkPolicy.shouldCancelSimulatedFireworkOnGlideStop(true, false, true));
        assertFalse(ElytraFireworkPolicy.shouldCancelSimulatedFireworkOnGlideStop(true, true, false));
    }

    @Test
    void blockUseRedirectionRequiresTheToggleAndAllFireworkConditions() {
        assertTrue(ElytraFireworkPolicy.shouldRedirectBlockUse(true, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldRedirectBlockUse(false, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldRedirectBlockUse(true, false, true, true));
        assertFalse(ElytraFireworkPolicy.shouldRedirectBlockUse(true, true, false, true));
        assertFalse(ElytraFireworkPolicy.shouldRedirectBlockUse(true, true, true, false));
    }

    @Test
    void nonElytraBlockUseIsBlockedUntilTheElytraRouteIsAvailable() {
        assertTrue(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, true, false, false, true));
        assertTrue(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, true, true, false, true));
        assertTrue(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, true, false, true, true));
        assertFalse(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, true, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(false, true, false, false, true));
        assertFalse(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, false, false, false, true));
        assertFalse(ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(true, true, false, false, false));
    }

    @Test
    void targetSpeedUsesMetersPerSecondAndVanillaDamping() {
        Vec3d result = ElytraFireworkPolicy.applyTargetSpeed(
                new Vec3d(0.0, 0.0, 1.6),
                Vec3d.ZERO,
                new Vec3d(0.0, 0.0, 1.0),
                64.0,
                true);

        assertEquals(0.0, result.x, 1.0E-9);
        assertEquals(0.0, result.y, 1.0E-9);
        assertEquals(1.6, result.z, 1.0E-9);
    }

    @Test
    void targetSpeedPreservesVanillaVelocityWhenDisabled() {
        Vec3d vanilla = new Vec3d(0.25, -0.5, 1.75);
        assertEquals(
                vanilla,
                ElytraFireworkPolicy.applyTargetSpeed(
                        vanilla,
                        new Vec3d(5.0, 6.0, 7.0),
                        new Vec3d(0.0, 1.0, 0.0),
                        20.0,
                        false));
    }

    @Test
    void targetSpeedCanDampToAConfiguredZero() {
        Vec3d result = ElytraFireworkPolicy.applyTargetSpeed(
                Vec3d.ZERO,
                new Vec3d(2.0, -4.0, 6.0),
                new Vec3d(0.0, 0.0, 1.0),
                0.0,
                true);

        assertEquals(1.0, result.x, 1.0E-9);
        assertEquals(-2.0, result.y, 1.0E-9);
        assertEquals(3.0, result.z, 1.0E-9);
    }
}
