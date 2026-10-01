package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.PotionEffectPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerEntity.class, priority = 2100)
public abstract class PlayerEntityPotionEffectsMixin {
    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float bettermoving$simulateSpeedPotion(float movementSpeed) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ClientPlayerEntity)
                || MinecraftClient.getInstance().player != player) {
            return movementSpeed;
        }

        movementSpeed = bettermoving$removeSlowness(movementSpeed, player);

        StatusEffectInstance effect = player.getStatusEffect(StatusEffects.SPEED);
        int actualLevel = effect == null ? 0 : effect.getAmplifier() + 1;
        return PotionEffectPolicy.applySimulatedSpeed(
                movementSpeed,
                actualLevel,
                BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue(),
                BetterMovingConfigs.simulatePotionEffects(),
                BetterMovingConfigs.overridePotionEffects());
    }

    private float bettermoving$removeSlowness(float movementSpeed, PlayerEntity player) {
        if (!BetterMovingConfigs.ignoreLevitationAndSlowness()
                || !player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            return movementSpeed;
        }

        EntityAttributeModifier template = StatusEffects.SLOWNESS
                .getAttributeModifiers()
                .get(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance attribute = player.getAttributeInstance(
                EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (template == null || attribute == null) {
            return movementSpeed;
        }

        EntityAttributeModifier modifier = attribute.getModifier(template.getId());
        if (modifier == null || modifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
            return movementSpeed;
        }
        return PotionEffectPolicy.removeSlowness(movementSpeed, modifier.getValue(), true);
    }
}
