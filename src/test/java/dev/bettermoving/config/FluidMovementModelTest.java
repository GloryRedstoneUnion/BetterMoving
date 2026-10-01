package dev.bettermoving.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FluidMovementModelTest {
    @Test
    void valuesHaveStableSerializedNames() {
        assertEquals("air", FluidMovementModel.AIR.getStringValue());
        assertEquals("water", FluidMovementModel.WATER.getStringValue());
    }

    @Test
    void valuesCycleInBothDirections() {
        assertEquals(FluidMovementModel.WATER, FluidMovementModel.AIR.cycle(true));
        assertEquals(FluidMovementModel.AIR, FluidMovementModel.WATER.cycle(true));
        assertEquals(FluidMovementModel.WATER, FluidMovementModel.AIR.cycle(false));
    }

    @Test
    void deserializationIsCaseInsensitiveAndFallsBackToAir() {
        assertEquals(FluidMovementModel.WATER, FluidMovementModel.AIR.fromString("WATER"));
        assertEquals(FluidMovementModel.AIR, FluidMovementModel.WATER.fromString("unknown"));
    }
}
