package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.physics.ClientRiptide;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerRiptideMixin {
    @WrapOperation(
            method = "onTrackedDataSet",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;clearActiveItem()V"))
    private void bettermoving$preserveLocalTridentCharge(
            ClientPlayerEntity player, Operation<Void> original) {
        // The server never started this charge. Unrelated living-flag updates
        // must not clear it just because the server's USING_ITEM flag is false.
        if (!ClientRiptide.isCharging(player)) {
            original.call(player);
        }
    }

    @Inject(method = "clearActiveItem", at = @At("RETURN"))
    private void bettermoving$clearLocalTridentCharge(CallbackInfo ci) {
        ClientRiptide.clearCharge((ClientPlayerEntity) (Object) this);
    }
}
