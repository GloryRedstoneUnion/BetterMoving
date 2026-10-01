package dev.bettermoving.compat;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reproduces ViaFabricPlus 2.8.7's competing water-check redirect.
 */
@Mixin(value = ClientPlayerEntity.class, priority = 2000)
public abstract class ViaFabricPlusClientPlayerConflictMixin {
    @Redirect(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isTouchingWater()Z"))
    private boolean viafabricplus$redirectTickMovement(ClientPlayerEntity player) {
        return player.isTouchingWater();
    }
}
