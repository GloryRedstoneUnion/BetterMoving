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
    private boolean fluidair$useAirTravelInWater(boolean touchingWater) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                fluidair$isLocalPlayer(),
                touchingWater);
    }

    @ModifyExpressionValue(
            method = {"tickMovement", "travel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z"))
    private boolean fluidair$useAirTravelInLava(boolean inLava) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                fluidair$isLocalPlayer(),
                inLava);
    }

    @Unique
    private boolean fluidair$isLocalPlayer() {
        return (Object) this instanceof ClientPlayerEntity;
    }
}
