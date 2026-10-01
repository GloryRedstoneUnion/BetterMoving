package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
                0.5);

        assertEquals(0.375, velocity.x, 1.0E-12);
        assertEquals(-0.15, velocity.y, 1.0E-12);
        assertEquals(0.75, velocity.z, 1.0E-12);
    }

    @Test
    void boostCanBeGreaterThanOne() {
        Vec3d velocity = MovementSpeedPolicy.apply(new Vec3d(0.25, 0.0, 0.5), 2.0);

        assertEquals(0.75, velocity.x, 1.0E-12);
        assertEquals(0.0, velocity.y, 1.0E-12);
        assertEquals(1.5, velocity.z, 1.0E-12);
    }

    @Test
    void nonFiniteBoostDoesNotCorruptVelocity() {
        Vec3d velocity = new Vec3d(0.25, -0.1, 0.5);

        assertSame(velocity, MovementSpeedPolicy.apply(velocity, Double.NaN));
        assertSame(velocity, MovementSpeedPolicy.apply(velocity, Double.POSITIVE_INFINITY));
    }
}
