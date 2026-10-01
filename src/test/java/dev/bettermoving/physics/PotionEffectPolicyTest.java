package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PotionEffectPolicyTest {
    @Test
    void simulatedSpeedUsesTheVanillaTwentyPercentPerLevelFormula() {
        assertEquals(
                0.16F,
                PotionEffectPolicy.applySimulatedSpeed(0.1F, 0, 3, true),
                1.0E-6F);
    }

    @Test
    void simulatedSpeedRaisesExistingEffectsToTheConfiguredLevel() {
        assertEquals(
                0.16F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 2, 3, true),
                1.0E-6F);
        assertEquals(
                0.14F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 2, 1, true),
                1.0E-6F);
    }

    @Test
    void simulatedSpeedUsesTheVanillaTotalMultiplierWithOtherModifiers() {
        assertEquals(
                0.28F,
                PotionEffectPolicy.applySimulatedSpeed(0.24F, 1, 2, true),
                1.0E-6F);
    }

    @Test
    void simulatedJumpBoostUsesTheVanillaPointOnePerLevelFormula() {
        assertEquals(
                0.3F,
                PotionEffectPolicy.applySimulatedJumpBoost(0.0F, 0, 3, true),
                1.0E-6F);
    }

    @Test
    void disabledSimulationPreservesBothCalculations() {
        assertEquals(
                0.14F,
                PotionEffectPolicy.applySimulatedSpeed(0.14F, 0, 3, false),
                1.0E-6F);
        assertEquals(
                0.2F,
                PotionEffectPolicy.applySimulatedJumpBoost(0.2F, 1, 3, false),
                1.0E-6F);
    }
}
