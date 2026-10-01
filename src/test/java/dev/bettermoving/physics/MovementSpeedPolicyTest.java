package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.bettermoving.config.MovementSpeedBoostMode;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

final class MovementSpeedPolicyTest {
    @Test
    void zeroBoostPreservesTheOriginalVelocity() {
        Vec3d velocity = new Vec3d(0.25, -0.1, 0.5);

        assertSame(velocity, MovementSpeedPolicy.apply(velocity, 0.0));
    }

    @Test
    void boostScalesTheCompleteVelocityVector() {
        Vec3d velocity = MovementSpeedPolicy.apply(
                new Vec3d(0.25, -0.1, 0.5),
                0.5,
                MovementSpeedBoostMode.ALL_AXES);

        assertEquals(0.375, velocity.x, 1.0E-12);
        assertEquals(-0.15, velocity.y, 1.0E-12);
        assertEquals(0.75, velocity.z, 1.0E-12);
    }

    @Test
    void boostCanBeGreaterThanOne() {
        Vec3d velocity = MovementSpeedPolicy.apply(
                new Vec3d(0.25, 0.0, 0.5),
                2.0,
                MovementSpeedBoostMode.HORIZONTAL);

        assertEquals(0.75, velocity.x, 1.0E-12);
        assertEquals(0.0, velocity.y, 1.0E-12);
        assertEquals(1.5, velocity.z, 1.0E-12);
    }

    @Test
    void nonFiniteBoostDoesNotCorruptVelocity() {
        Vec3d velocity = new Vec3d(0.25, -0.1, 0.5);

        assertSame(velocity, MovementSpeedPolicy.apply(
                velocity, Double.NaN, MovementSpeedBoostMode.ALL_AXES));
        assertSame(velocity, MovementSpeedPolicy.apply(
                velocity, Double.POSITIVE_INFINITY, MovementSpeedBoostMode.ALL_AXES));
    }

    @Test
    void horizontalModeLeavesVerticalVelocityUnchanged() {
        Vec3d velocity = MovementSpeedPolicy.apply(
                new Vec3d(0.25, -0.1, 0.5),
                0.5,
                MovementSpeedBoostMode.HORIZONTAL);

        assertEquals(0.375, velocity.x, 1.0E-12);
        assertEquals(-0.1, velocity.y, 1.0E-12);
        assertEquals(0.75, velocity.z, 1.0E-12);
    }

    @Test
    void verticalModeLeavesHorizontalVelocityUnchanged() {
        Vec3d velocity = MovementSpeedPolicy.apply(
                new Vec3d(0.25, -0.1, 0.5),
                0.5,
                MovementSpeedBoostMode.VERTICAL);

        assertEquals(0.25, velocity.x, 1.0E-12);
        assertEquals(-0.15, velocity.y, 1.0E-12);
        assertEquals(0.5, velocity.z, 1.0E-12);
    }
}
