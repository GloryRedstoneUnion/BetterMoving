package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.fluidair.physics.FluidMovementContext;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayerEntity.class, priority = 2100)
public abstract class ClientPlayerMovementMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void fluidair$enterMovementContext(CallbackInfo ci) {
        FluidMovementContext.enter((ClientPlayerEntity) (Object) this);
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void fluidair$exitMovementContext(CallbackInfo ci) {
        FluidMovementContext.exit((ClientPlayerEntity) (Object) this);
    }

    @ModifyReturnValue(method = "isSubmergedInWater", at = @At("RETURN"))
    private boolean fluidair$resolveSubmergedInWater(boolean detectedWaterState) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        return FluidMovementContext.resolveSubmergedWaterState(
                player,
                detectedWaterState);
    }
}
