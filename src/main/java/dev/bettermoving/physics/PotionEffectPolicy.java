package dev.bettermoving.physics;

public final class PotionEffectPolicy {
    public static final float SPEED_PER_LEVEL = 0.2F;
    public static final float JUMP_BOOST_PER_LEVEL = 0.1F;

    private PotionEffectPolicy() {
    }

    public static float applySimulatedSpeed(
            float movementSpeed,
            int actualLevel,
            int simulatedLevel,
            boolean enabled) {
        if (!enabled) {
            return movementSpeed;
        }

        int effectiveLevel = Math.max(actualLevel, simulatedLevel);
        if (effectiveLevel == actualLevel) {
            return movementSpeed;
        }

        float actualMultiplier = 1.0F + SPEED_PER_LEVEL * actualLevel;
        float effectiveMultiplier = 1.0F + SPEED_PER_LEVEL * effectiveLevel;
        return movementSpeed / actualMultiplier * effectiveMultiplier;
    }

    public static float applySimulatedJumpBoost(
            float jumpBoostModifier,
            int actualLevel,
            int simulatedLevel,
            boolean enabled) {
        int additionalLevels = getAdditionalLevels(actualLevel, simulatedLevel, enabled);
        return jumpBoostModifier + JUMP_BOOST_PER_LEVEL * additionalLevels;
    }

    private static int getAdditionalLevels(int actualLevel, int simulatedLevel, boolean enabled) {
        if (!enabled) {
            return 0;
        }

        return Math.max(0, simulatedLevel - actualLevel);
    }
}
