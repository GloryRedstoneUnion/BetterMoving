package dev.bettermoving.physics;

import dev.bettermoving.config.MovementSpeedBoostMode;
import net.minecraft.util.math.Vec3d;

public final class MovementSpeedPolicy {
    private MovementSpeedPolicy() {
    }

    public static Vec3d apply(Vec3d velocity, double boost) {
        return apply(velocity, boost, MovementSpeedBoostMode.ALL_AXES);
    }

    public static Vec3d apply(Vec3d velocity, double boost, MovementSpeedBoostMode mode) {
        if (boost == 0.0 || !Double.isFinite(boost)) {
            return velocity;
        }

        double multiplier = 1.0 + boost;
        return switch (mode) {
            case HORIZONTAL -> new Vec3d(
                    velocity.x * multiplier,
                    velocity.y,
                    velocity.z * multiplier);
            case VERTICAL -> new Vec3d(
                    velocity.x,
                    velocity.y * multiplier,
                    velocity.z);
            case ALL_AXES -> velocity.multiply(multiplier);
        };
    }
}
