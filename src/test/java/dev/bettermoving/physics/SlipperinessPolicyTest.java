package dev.bettermoving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SlipperinessPolicyTest {
    @Test
    void disabledFeaturePreservesVanillaSlipperiness() {
        assertEquals(0.98F, SlipperinessPolicy.resolve(false, true, 0.98F));
    }

    @Test
    void enabledFeatureNormalizesLocalSlipperyBlocks() {
        assertEquals(0.6F, SlipperinessPolicy.resolve(true, true, 0.98F));
    }

    @Test
    void enabledFeaturePreservesNormalAndLowSlipperiness() {
        assertEquals(0.6F, SlipperinessPolicy.resolve(true, true, 0.6F));
        assertEquals(0.4F, SlipperinessPolicy.resolve(true, true, 0.4F));
    }

    @Test
    void enabledFeatureDoesNotChangeOtherEntities() {
        assertEquals(0.98F, SlipperinessPolicy.resolve(true, false, 0.98F));
    }
}
