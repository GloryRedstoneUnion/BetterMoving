package dev.bettermoving.physics;

public final class SlipperinessPolicy {
    public static final float NORMAL_BLOCK_SLIPPERINESS = 0.6F;

    private SlipperinessPolicy() {
    }

    public static float resolve(
            boolean enabled,
            boolean localPlayer,
            float slipperiness) {
        if (!enabled || !localPlayer) {
            return slipperiness;
        }
        return Math.min(slipperiness, NORMAL_BLOCK_SLIPPERINESS);
    }
}
