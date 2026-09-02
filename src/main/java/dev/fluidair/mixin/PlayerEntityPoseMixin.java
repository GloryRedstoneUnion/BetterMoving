package dev.fluidair.mixin;

import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityPoseMixin {
    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void fluidair$preventSwimmingPose(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ClientPlayerEntity
                && FluidAirConfigs.ignoreFluidPhysics()) {
            player.setSwimming(FluidAirMovementPolicy.resolveSwimmingPose(
                    true,
                    player.isSwimming()));
            ci.cancel();
        }
    }
}
