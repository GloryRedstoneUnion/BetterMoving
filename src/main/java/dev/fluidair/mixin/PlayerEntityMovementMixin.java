package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import dev.fluidair.physics.FluidMovementContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class, priority = 2100)
public abstract class PlayerEntityMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void fluidair$enterTravelContext(Vec3d movementInput, CallbackInfo ci) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void fluidair$exitTravelContext(Vec3d movementInput, CallbackInfo ci) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"))
    private void fluidair$enterSwimmingContext(CallbackInfo ci) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "updateSwimming", at = @At("RETURN"))
    private void fluidair$exitSwimmingContext(CallbackInfo ci) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @Inject(method = "checkFallFlying", at = @At("HEAD"))
    private void fluidair$enterFallFlyingContext(CallbackInfoReturnable<Boolean> cir) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "checkFallFlying", at = @At("RETURN"))
    private void fluidair$exitFallFlyingContext(CallbackInfoReturnable<Boolean> cir) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @ModifyExpressionValue(
            method = {"travel", "increaseTravelMotionStats"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isSwimming()Z"))
    private boolean fluidair$resolveSwimmingMovement(boolean swimming) {
        return fluidair$resolveWaterMovementState(swimming, false);
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean fluidair$resolveSubmergedMovementStats(boolean submerged) {
        return fluidair$resolveWaterMovementState(
                submerged,
                ((PlayerEntity) (Object) this).isSubmergedIn(FluidTags.LAVA));
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$resolveTouchingWaterMovementStats(boolean touchingWater) {
        return fluidair$resolveTouchingWater(touchingWater);
    }

    @Unique
    private boolean fluidair$resolveTouchingWater(boolean touchingWater) {
        return fluidair$resolveWaterMovementState(
                touchingWater,
                ((PlayerEntity) (Object) this).isInLava());
    }

    @Unique
    private boolean fluidair$resolveWaterMovementState(
            boolean detectedWaterState,
            boolean detectedOtherFluidState) {
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                (Object) this instanceof ClientPlayerEntity,
                detectedWaterState,
                detectedOtherFluidState);
    }
}
