package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.FluidTags;
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
        return fluidair$shouldApplyWaterMovement() ? velocity : this.getVelocity();
    }

    @ModifyArg(
            method = "updateWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z"),
            index = 1)
    private double fluidair$useWaterCurrentSpeedInLava(double vanillaSpeed) {
        return FluidAirMovementPolicy.resolveLavaCurrentSpeed(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                vanillaSpeed);
    }

    @ModifyExpressionValue(
            method = "getVelocityMultiplier",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean fluidair$resolveFluidVelocityMultiplier(boolean fluidBlock) {
        return fluidair$resolveTouchingWater(fluidBlock);
    }

    @ModifyExpressionValue(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isTouchingWater()Z"))
    private boolean fluidair$resolveSwimmingInWater(boolean touchingWater) {
        return fluidair$resolveTouchingWater(touchingWater);
    }

    @ModifyExpressionValue(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isSubmergedInWater()Z"))
    private boolean fluidair$resolveSwimmingWhenSubmerged(boolean submergedInWater) {
        return fluidair$resolveSubmergedInWater(submergedInWater);
    }

    @ModifyExpressionValue(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/fluid/FluidState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean fluidair$treatLavaBlockAsWaterForSwimming(boolean waterAtFeet) {
        return fluidair$resolveTouchingWater(waterAtFeet);
    }

    @WrapOperation(
            method = "checkWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;onLanding()V"))
    private void fluidair$preserveFallDistanceInWater(
            Entity entity,
            Operation<Void> original) {
        if (fluidair$shouldApplyWaterMovement()) {
            original.call(entity);
        }
    }

    @ModifyExpressionValue(
            method = "baseTick",
            at = @At(value = "CONSTANT", args = "floatValue=0.5"))
    private float fluidair$resolveFallDistanceInLava(float vanillaMultiplier) {
        if (FluidAirMovementPolicy.shouldResetLavaFallDistanceAsWater(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer())) {
            ((Entity) (Object) this).onLanding();
        }
        return FluidAirMovementPolicy.resolveLavaFallDistanceMultiplier(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                vanillaMultiplier);
    }

    @Inject(
            method = {"onBubbleColumnSurfaceCollision", "onBubbleColumnCollision"},
            at = @At("HEAD"),
            cancellable = true)
    private void fluidair$resolveBubbleColumnMovement(boolean drag, CallbackInfo ci) {
        if (!FluidAirMovementPolicy.shouldApplyBubbleColumnMovement(
                FluidAirConfigs.ignoreFluidPhysics(),
                fluidair$isLocalPlayer())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean fluidair$shouldApplyWaterMovement() {
        return FluidAirMovementPolicy.shouldApplyWaterMovement(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer());
    }

    @Unique
    private boolean fluidair$resolveTouchingWater(boolean detectedWaterState) {
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                detectedWaterState,
                ((Entity) (Object) this).isInLava());
    }

    @Unique
    private boolean fluidair$resolveSubmergedInWater(boolean detectedWaterState) {
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                fluidair$isLocalPlayer(),
                detectedWaterState,
                ((Entity) (Object) this).isSubmergedIn(FluidTags.LAVA));
    }

    @Unique
    private boolean fluidair$isLocalPlayer() {
        return (Object) this instanceof ClientPlayerEntity;
    }
}
