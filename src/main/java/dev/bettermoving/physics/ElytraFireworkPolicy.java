package dev.bettermoving.physics;

public final class ElytraFireworkPolicy {
    private ElytraFireworkPolicy() {
    }

    public static boolean shouldSimulate(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean holdingFirework) {
        return enabled && localPlayer && fallFlying && holdingFirework;
    }

    public static boolean shouldRedirectBlockUse(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean holdingFirework) {
        return enabled && localPlayer && fallFlying && holdingFirework;
    }

    public static boolean shouldBlockNonElytraBlockUse(
            boolean enabled,
            boolean localPlayer,
            boolean fallFlying,
            boolean elytraBlockUseEnabled,
            boolean holdingFirework) {
        return enabled
                && localPlayer
                && holdingFirework
                && (!fallFlying || !elytraBlockUseEnabled);
    }
}
