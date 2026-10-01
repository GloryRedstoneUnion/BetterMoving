package dev.bettermoving.mixin;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.MovementSpeedPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 100)
public abstract class ClientPlayerMovementSpeedMixin {
    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void bettermoving$applyMovementSpeedBoost(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        player.setVelocity(MovementSpeedPolicy.apply(
                player.getVelocity(),
                BetterMovingConfigs.MOVEMENT_SPEED_BOOST.getDoubleValue()));
    }
}
