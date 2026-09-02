package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
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
    private boolean fluidair$useAirMovementInsteadOfSwimming(boolean swimming) {
        return fluidair$resolveFluidMovementState(swimming);
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean fluidair$recordMovementAsAirWhenSubmerged(boolean submerged) {
        return fluidair$resolveFluidMovementState(submerged);
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$recordMovementAsAirWhenTouchingWater(boolean touchingWater) {
        return fluidair$resolveFluidMovementState(touchingWater);
    }

    @ModifyExpressionValue(
            method = "checkFallFlying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$allowFallFlyingInWater(boolean touchingWater) {
        return fluidair$resolveFluidMovementState(touchingWater);
    }

    @Unique
    private boolean fluidair$resolveFluidMovementState(boolean detectedFluidState) {
        return FluidAirMovementPolicy.resolveFluidMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                (Object) this instanceof ClientPlayerEntity,
                detectedFluidState);
    }
}
