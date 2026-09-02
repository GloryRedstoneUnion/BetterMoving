package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 2100)
public abstract class LivingEntityFluidPhysicsMixin {
    @ModifyExpressionValue(
            method = {"tickMovement", "travel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isTouchingWater()Z"))
    private boolean fluidair$resolveWaterTravel(boolean touchingWater) {
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                touchingWater,
                ((LivingEntity) (Object) this).isInLava());
    }

    @ModifyExpressionValue(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z"))
    private boolean fluidair$resolveLavaTravel(boolean inLava) {
        return FluidAirMovementPolicy.resolveLavaMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                inLava);
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z",
                    ordinal = 0))
    private boolean fluidair$selectEffectiveFluidHeight(boolean inLava) {
        return FluidAirMovementPolicy.resolveLavaFluidHeightState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                inLava);
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z",
                    ordinal = 1))
    private boolean fluidair$disableLavaJumpBranch(boolean inLava) {
        return FluidAirMovementPolicy.resolveLavaMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                inLava);
    }

    @Unique
    private boolean fluidair$isLocalPlayer() {
        return (Object) this instanceof ClientPlayerEntity;
    }
}
