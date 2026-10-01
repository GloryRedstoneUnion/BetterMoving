package dev.bettermoving.physics;

import net.minecraft.util.math.Vec3d;

public final class MovementSpeedPolicy {
    private MovementSpeedPolicy() {
    }

    public static Vec3d apply(Vec3d velocity, double boost) {
        if (boost == 0.0 || !Double.isFinite(boost)) {
            return velocity;
        }
        return velocity.multiply(1.0 + boost);
    }
}
