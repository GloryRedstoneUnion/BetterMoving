package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.LevitationElytraFlight;
import dev.bettermoving.physics.PotionEffectPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 2100)
public abstract class LivingEntityMovementEffectsMixin {
    @ModifyReturnValue(method = "isFallFlying", at = @At("RETURN"))
    private boolean bettermoving$preserveLocalLevitationFlight(boolean trackedFlight) {
        return LevitationElytraFlight.resolve((LivingEntity) (Object) this, trackedFlight);
    }

    @WrapOperation(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private boolean bettermoving$ignoreLevitationInTravel(
            LivingEntity entity,
            StatusEffect effect,
            Operation<Boolean> original) {
        return bettermoving$resolveLevitation(entity, effect, original);
    }

    @WrapOperation(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private boolean bettermoving$ignoreLevitationInTickMovement(
            LivingEntity entity,
            StatusEffect effect,
            Operation<Boolean> original) {
        return bettermoving$resolveLevitation(entity, effect, original);
    }

    @WrapOperation(
            method = "tickFallFlying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private boolean bettermoving$ignoreLevitationInFallFlying(
            LivingEntity entity,
            StatusEffect effect,
            Operation<Boolean> original) {
        return bettermoving$resolveLevitation(entity, effect, original);
    }

    private boolean bettermoving$resolveLevitation(
            LivingEntity entity,
            StatusEffect effect,
            Operation<Boolean> original) {
        boolean detected = original.call(entity, effect);
        boolean localPlayer = entity instanceof ClientPlayerEntity
                && MinecraftClient.getInstance().player == entity;
        return effect == StatusEffects.LEVITATION
                ? PotionEffectPolicy.resolveLevitation(
                        detected,
                        BetterMovingConfigs.ignoreLevitationAndSlowness(),
                        localPlayer)
                : effect == StatusEffects.DOLPHINS_GRACE
                        ? PotionEffectPolicy.resolveDolphinsGrace(
                                detected,
                                BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getIntegerValue(),
                                BetterMovingConfigs.simulatePotionEffects() && localPlayer,
                                BetterMovingConfigs.overridePotionEffects() && localPlayer)
                : detected;
    }
}
