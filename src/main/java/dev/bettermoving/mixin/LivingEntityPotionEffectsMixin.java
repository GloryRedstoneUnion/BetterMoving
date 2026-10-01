package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.PotionEffectPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 2100)
public abstract class LivingEntityPotionEffectsMixin {
    @ModifyReturnValue(method = "getJumpBoostVelocityModifier", at = @At("RETURN"))
    private float bettermoving$simulateJumpBoost(float jumpBoostModifier) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof ClientPlayerEntity)
                || MinecraftClient.getInstance().player != entity) {
            return jumpBoostModifier;
        }

        StatusEffectInstance effect = entity.getStatusEffect(StatusEffects.JUMP_BOOST);
        int actualLevel = effect == null ? 0 : effect.getAmplifier() + 1;
        return PotionEffectPolicy.applySimulatedJumpBoost(
                jumpBoostModifier,
                actualLevel,
                BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue(),
                BetterMovingConfigs.simulatePotionEffects());
    }
}
