package dev.fluidair.physics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FluidAirMovementPolicyTest {
    @Test
    void enabledModeRejectsVanillaSwimmingPose() {
        assertFalse(FluidAirMovementPolicy.resolveSwimmingPose(true, true));
    }

    @Test
    void enabledModeRejectsFluidJumpLogic() {
        assertFalse(FluidAirMovementPolicy.useFluidJumpLogic(true));
    }

    @Test
    void enabledModeSuppressesFluidStateReportedByPreviousMixin() {
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementCheck(true, true));
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementCheck(true, false));
    }

    @Test
    void disabledModePreservesFluidStateReportedByPreviousMixin() {
        assertTrue(FluidAirMovementPolicy.resolveFluidMovementCheck(false, true));
        assertFalse(FluidAirMovementPolicy.resolveFluidMovementCheck(false, false));
    }

    @Test
    void disabledModePreservesVanillaBehavior() {
        assertTrue(FluidAirMovementPolicy.resolveSwimmingPose(false, true));
        assertTrue(FluidAirMovementPolicy.useFluidJumpLogic(false));
    }
}
