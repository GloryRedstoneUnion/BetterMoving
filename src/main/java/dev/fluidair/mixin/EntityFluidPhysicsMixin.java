package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 2100)
public abstract class EntityFluidPhysicsMixin {
    @Shadow
    public abstract Vec3d getVelocity();

    @ModifyArg(
            method = "updateMovementInFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"),
            index = 0)
    private Vec3d fluidair$ignoreFluidCurrent(Vec3d velocity) {
        return fluidair$shouldApplyFluidMovement() ? velocity : this.getVelocity();
    }

    @ModifyExpressionValue(
            method = "getVelocityMultiplier",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean fluidair$useAirVelocityMultiplier(boolean fluidBlock) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                (Object) this instanceof ClientPlayerEntity,
                fluidBlock);
    }

    @ModifyExpressionValue(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isTouchingWater()Z"))
    private boolean fluidair$preventSwimmingInWater(boolean touchingWater) {
        return fluidair$resolveFluidMovementState(touchingWater);
    }

    @ModifyExpressionValue(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isSubmergedInWater()Z"))
    private boolean fluidair$preventSwimmingWhenSubmerged(boolean submergedInWater) {
        return fluidair$resolveFluidMovementState(submergedInWater);
    }

    @WrapOperation(
            method = "checkWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;onLanding()V"))
    private void fluidair$preserveFallDistanceInWater(
            Entity entity,
            Operation<Void> original) {
        if (fluidair$shouldApplyFluidMovement()) {
            original.call(entity);
        }
    }

    @ModifyExpressionValue(
            method = "baseTick",
            at = @At(value = "CONSTANT", args = "floatValue=0.5"))
    private float fluidair$preserveFallDistanceInLava(float vanillaMultiplier) {
        return FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(
                FluidAirConfigs.ignoreFluidPhysics(),
                (Object) this instanceof ClientPlayerEntity,
                vanillaMultiplier);
    }

    @Inject(
            method = {"onBubbleColumnSurfaceCollision", "onBubbleColumnCollision"},
            at = @At("HEAD"),
            cancellable = true)
    private void fluidair$ignoreBubbleColumnMovement(boolean drag, CallbackInfo ci) {
        if (!fluidair$shouldApplyFluidMovement()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean fluidair$shouldApplyFluidMovement() {
        return FluidAirMovementPolicy.shouldApplyFluidMovement(
                FluidAirConfigs.ignoreFluidPhysics(),
                (Object) this instanceof ClientPlayerEntity);
    }

    @Unique
    private boolean fluidair$resolveFluidMovementState(boolean detectedFluidState) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                (Object) this instanceof ClientPlayerEntity,
                detectedFluidState);
    }
}
