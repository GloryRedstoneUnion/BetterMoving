package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientPlayerEntity.class, priority = 2100)
public abstract class ClientPlayerMovementMixin {
    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$ignoreWaterForSprinting(boolean touchingWater) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                true,
                touchingWater);
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSubmergedInWater()Z"))
    private boolean fluidair$ignoreSubmersionForSprinting(boolean submergedInWater) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                true,
                submergedInWater);
    }

    @ModifyExpressionValue(
            method = "isWalking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSubmergedInWater()Z"))
    private boolean fluidair$useAirSprintingThreshold(boolean submergedInWater) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                true,
                submergedInWater);
    }
}
