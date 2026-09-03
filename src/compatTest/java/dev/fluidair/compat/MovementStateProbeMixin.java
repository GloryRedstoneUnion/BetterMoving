package dev.fluidair.compat;

import dev.fluidair.test.MovementCompatProbe;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 1900)
public abstract class MovementStateProbeMixin {
    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldSlowDown()Z"))
    private void fluidairTest$observeWaterStateInsideMovement(CallbackInfo ci) {
        MovementCompatProbe.observeMovementWaterState((ClientPlayerEntity) (Object) this);
    }
}
