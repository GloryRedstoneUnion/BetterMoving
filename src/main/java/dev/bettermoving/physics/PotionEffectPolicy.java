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
            boolean enabled,
            boolean overrideExistingEffect) {
        if (!enabled) {
            return movementSpeed;
        }

        int effectiveLevel = overrideExistingEffect
                ? simulatedLevel
                : Math.max(actualLevel, simulatedLevel);
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
            boolean enabled,
            boolean overrideExistingEffect) {
        if (!enabled) {
            return jumpBoostModifier;
        }

        int effectiveLevel = overrideExistingEffect
                ? simulatedLevel
                : Math.max(actualLevel, simulatedLevel);
        return jumpBoostModifier + JUMP_BOOST_PER_LEVEL * (effectiveLevel - actualLevel);
    }

    public static float removeSlowness(
            float movementSpeed,
            double slownessModifierAmount,
            boolean enabled) {
        if (!enabled) {
            return movementSpeed;
        }

        double multiplier = 1.0 + slownessModifierAmount;
        if (Math.abs(multiplier) < 1.0E-6) {
            return movementSpeed;
        }
        return (float) (movementSpeed / multiplier);
    }

    public static boolean resolveLevitation(
            boolean detectedLevitation,
            boolean enabled,
            boolean localPlayer) {
        return detectedLevitation && !(enabled && localPlayer);
    }
}
