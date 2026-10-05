package dev.bettermoving.mixin;

import dev.bettermoving.entity.ClientFireworkRocketManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels client-only simulated Elytra fireworks when the local glide flag is
 * cleared, including state changes received through the entity data tracker.
 */
@Mixin(Entity.class)
public abstract class EntityFallFlyingMixin {
    @Unique
    private boolean bettermoving$wasFallFlying;

    @Inject(method = "setFlag", at = @At("HEAD"))
    private void bettermoving$cancelFireworkBeforeGlideFlagClears(
            int index,
            boolean value,
            CallbackInfo ci) {
        if (index != 7
                || value
                || !((Object) this instanceof ClientPlayerEntity player)
                || player != MinecraftClient.getInstance().player
                || !player.isFallFlying()) {
            return;
        }
        ClientFireworkRocketManager.cancelOnGlideStop(player, true);
    }

    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void bettermoving$cancelFireworkAfterTrackedGlideStateChange(
            TrackedData<?> data,
            CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity player)
                || player != MinecraftClient.getInstance().player) {
            return;
        }

        boolean fallFlying = player.isFallFlying();
        if (this.bettermoving$wasFallFlying && !fallFlying) {
            ClientFireworkRocketManager.cancelOnGlideStop(player, true);
        }
        this.bettermoving$wasFallFlying = fallFlying;
    }
}
