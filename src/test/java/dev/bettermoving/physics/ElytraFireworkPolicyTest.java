package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ElytraFireworkPolicyTest {
    @Test
    void simulationRequiresTheToggleAndAllFireworkConditions() {
        assertTrue(ElytraFireworkPolicy.shouldSimulate(true, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(false, true, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, false, true, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, true, false, true));
        assertFalse(ElytraFireworkPolicy.shouldSimulate(true, true, true, false));
    }
}
