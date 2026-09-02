package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 2100)
public abstract class PlayerEntityMovementMixin {
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

    @ModifyExpressionValue(
            method = "checkFallFlying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$resolveFallFlyingInWater(boolean touchingWater) {
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
