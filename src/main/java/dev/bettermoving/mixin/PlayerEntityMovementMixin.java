package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.BetterMovingMovementPolicy;
import dev.bettermoving.physics.FluidMovementContext;
import dev.bettermoving.physics.LevitationElytraFlight;
import dev.bettermoving.physics.PotionEffectPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEntity.class, priority = 2100)
public abstract class PlayerEntityMovementMixin {
    @Inject(method = "startFallFlying", at = @At("RETURN"))
    private void bettermoving$startLocalLevitationFlight(CallbackInfo ci) {
        LevitationElytraFlight.start((PlayerEntity) (Object) this);
    }

    @Inject(method = "stopFallFlying", at = @At("RETURN"))
    private void bettermoving$stopLocalLevitationFlight(CallbackInfo ci) {
        // Vanilla briefly sets the flag to true and triggers tracked-data callbacks.
        LevitationElytraFlight.stop((PlayerEntity) (Object) this);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void bettermoving$enterTravelContext(Vec3d movementInput, CallbackInfo ci) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void bettermoving$exitTravelContext(Vec3d movementInput, CallbackInfo ci) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"))
    private void bettermoving$enterSwimmingContext(CallbackInfo ci) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "updateSwimming", at = @At("RETURN"))
    private void bettermoving$exitSwimmingContext(CallbackInfo ci) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @Inject(method = "checkFallFlying", at = @At("HEAD"))
    private void bettermoving$enterFallFlyingContext(CallbackInfoReturnable<Boolean> cir) {
        FluidMovementContext.enter((PlayerEntity) (Object) this);
    }

    @Inject(method = "checkFallFlying", at = @At("RETURN"))
    private void bettermoving$exitFallFlyingContext(CallbackInfoReturnable<Boolean> cir) {
        FluidMovementContext.exit((PlayerEntity) (Object) this);
    }

    @ModifyExpressionValue(
            method = {"travel", "increaseTravelMotionStats"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isSwimming()Z"))
    private boolean bettermoving$resolveSwimmingMovement(boolean swimming) {
        return bettermoving$resolveWaterMovementState(swimming, false);
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean bettermoving$resolveSubmergedMovementStats(boolean submerged) {
        return bettermoving$resolveWaterMovementState(
                submerged,
                ((PlayerEntity) (Object) this).isSubmergedIn(FluidTags.LAVA));
    }

    @ModifyExpressionValue(
            method = "increaseTravelMotionStats",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWater()Z"))
    private boolean bettermoving$resolveTouchingWaterMovementStats(boolean touchingWater) {
        return bettermoving$resolveTouchingWater(touchingWater);
    }

    @Unique
    private boolean bettermoving$resolveTouchingWater(boolean touchingWater) {
        return bettermoving$resolveWaterMovementState(
                touchingWater,
                ((PlayerEntity) (Object) this).isInLava());
    }

    @Unique
    private boolean bettermoving$resolveWaterMovementState(
            boolean detectedWaterState,
            boolean detectedOtherFluidState) {
        return BetterMovingMovementPolicy.resolveWaterMovementState(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                (Object) this instanceof ClientPlayerEntity,
                detectedWaterState,
                detectedOtherFluidState);
    }

    @WrapOperation(
            method = "checkFallFlying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private boolean bettermoving$ignoreLevitationInFallFlyingCheck(
            PlayerEntity entity,
            StatusEffect effect,
            Operation<Boolean> original) {
        boolean detected = original.call(entity, effect);
        return effect == StatusEffects.LEVITATION
                ? PotionEffectPolicy.resolveLevitation(
                        detected,
                        BetterMovingConfigs.ignoreLevitationAndSlowness(),
                        entity instanceof ClientPlayerEntity
                                && MinecraftClient.getInstance().player == entity)
                : detected;
    }
}
