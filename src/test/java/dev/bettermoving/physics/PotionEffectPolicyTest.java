package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PotionEffectPolicyTest {
    @Test
    void simulatedSpeedUsesTheVanillaTwentyPercentPerLevelFormula() {
        assertEquals(
                0.16F,
                PotionEffectPolicy.applySimulatedSpeed(0.1F, 0, 3, true, false),
                1.0E-6F);
    }

    @Test
    void simulatedSpeedRaisesExistingEffectsToTheConfiguredLevel() {
        assertEquals(
                0.16F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 2, 3, true, false),
                1.0E-6F);
        assertEquals(
                0.14F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 2, 1, true, false),
                1.0E-6F);
    }

    @Test
    void simulatedSpeedUsesTheVanillaTotalMultiplierWithOtherModifiers() {
        assertEquals(
                0.28F,
                PotionEffectPolicy.applySimulatedSpeed(0.24F, 1, 2, true, false),
                1.0E-6F);
    }

    @Test
    void overrideReplacesAnExistingSpeedEffect() {
        assertEquals(
                0.12F,
                PotionEffectPolicy.applySimulatedSpeed(0.16F, 3, 1, true, true),
                1.0E-6F);
    }

    @Test
    void simulatedJumpBoostUsesTheVanillaPointOnePerLevelFormula() {
        assertEquals(
                0.3F,
                PotionEffectPolicy.applySimulatedJumpBoost(0.0F, 0, 3, true, false),
                1.0E-6F);
    }

    @Test
    void overrideReplacesAnExistingJumpBoostEffect() {
        assertEquals(
                0.1F,
                PotionEffectPolicy.applySimulatedJumpBoost(0.3F, 3, 1, true, true),
                1.0E-6F);
    }

    @Test
    void simulatedDolphinsGraceActivatesAtAnyPositiveLevel() {
        assertEquals(
                true,
                PotionEffectPolicy.resolveDolphinsGrace(false, 1, true, false));
        assertEquals(
                true,
                PotionEffectPolicy.resolveDolphinsGrace(false, 255, true, false));
    }

    @Test
    void simulatedDolphinsGracePreservesExistingEffectWhenOverrideIsDisabled() {
        assertEquals(
                true,
                PotionEffectPolicy.resolveDolphinsGrace(true, 0, true, false));
    }

    @Test
    void overrideCanRemoveExistingDolphinsGraceEffect() {
        assertEquals(
                false,
                PotionEffectPolicy.resolveDolphinsGrace(true, 0, true, true));
    }

    @Test
    void disabledDolphinsGraceSimulationPreservesDetectedEffect() {
        assertEquals(
                true,
                PotionEffectPolicy.resolveDolphinsGrace(true, 0, false, true));
        assertEquals(
                false,
                PotionEffectPolicy.resolveDolphinsGrace(false, 1, false, false));
    }

    @Test
    void disabledSimulationPreservesBothCalculations() {
        assertEquals(
                0.14F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 0, 3, false, true),
                1.0E-6F);
        assertEquals(
                0.2F,
                PotionEffectPolicy.applySimulatedJumpBoost(0.2F, 1, 3, false, true),
                1.0E-6F);
    }

    @Test
    void ignoringSlownessRemovesOnlyItsTotalMultiplier() {
        assertEquals(
                0.1F,
                PotionEffectPolicy.removeSlowness(0.085F, -0.15, true),
                1.0E-6F);
    }

    @Test
    void disabledSlownessOverridePreservesMovementSpeed() {
        assertEquals(
                0.085F,
                PotionEffectPolicy.removeSlowness(0.085F, -0.15, false),
                1.0E-6F);
    }

    @Test
    void levitationIsIgnoredOnlyForTheLocalEnabledPlayer() {
        assertEquals(false, PotionEffectPolicy.resolveLevitation(true, true, true));
        assertEquals(true, PotionEffectPolicy.resolveLevitation(true, false, true));
        assertEquals(true, PotionEffectPolicy.resolveLevitation(true, true, false));
    }
}
