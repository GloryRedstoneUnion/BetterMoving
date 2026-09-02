package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientPlayerEntity.class, priority = 2100)
public abstract class ClientPlayerMovementMixin {
    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isTouchingWater()Z"))
    private boolean fluidair$resolveTouchingWaterForSprinting(boolean touchingWater) {
        return fluidair$resolveTouchingWater(touchingWater);
    }

    @ModifyExpressionValue(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSubmergedInWater()Z"))
    private boolean fluidair$resolveSubmersionForSprinting(boolean submergedInWater) {
        return fluidair$resolveSubmergedInWater(submergedInWater);
    }

    @ModifyExpressionValue(
            method = "isWalking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSubmergedInWater()Z"))
    private boolean fluidair$resolveWaterSprintingThreshold(boolean submergedInWater) {
        return fluidair$resolveSubmergedInWater(submergedInWater);
    }

    @ModifyExpressionValue(
            method = "shouldSlowDown",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isCrawling()Z"))
    private boolean fluidair$resolveCrawlingSlowdown(boolean crawling) {
        return crawling && !fluidair$resolveTouchingWater(false);
    }

    @Unique
    private boolean fluidair$resolveTouchingWater(boolean detectedWaterState) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                true,
                detectedWaterState,
                player.isInLava());
    }

    @Unique
    private boolean fluidair$resolveSubmergedInWater(boolean detectedWaterState) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        return FluidAirMovementPolicy.resolveWaterMovementState(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                true,
                detectedWaterState,
                player.isSubmergedIn(FluidTags.LAVA));
    }
}
