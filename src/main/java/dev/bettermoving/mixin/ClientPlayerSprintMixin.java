package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bettermoving.config.BetterMovingConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerSprintMixin {
    @ModifyExpressionValue(
            method = "canSprint",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/HungerManager;getFoodLevel()I"))
    private int bettermoving$ignoreSprintHunger(int foodLevel) {
        return (Object) this == MinecraftClient.getInstance().player
                        && BetterMovingConfigs.IGNORE_SPRINT_HUNGER.getBooleanValue()
                ? 20
                : foodLevel;
    }
}
