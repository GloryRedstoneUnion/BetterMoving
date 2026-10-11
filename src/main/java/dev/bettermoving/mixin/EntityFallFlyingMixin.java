package dev.bettermoving.mixin;

import dev.bettermoving.entity.ClientFireworkRocketManager;
import dev.bettermoving.physics.ElytraHover;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels client-only simulated Elytra fireworks when the local glide flag is
 * cleared and stops unpowered hovering flight when a rocket is removed.
 */
@Mixin(Entity.class)
public abstract class EntityFallFlyingMixin {
    @Unique
    private boolean bettermoving$wasFallFlying;

    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void bettermoving$hoverAfterRocketRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this instanceof FireworkRocketEntity rocket
                && rocket.getWorld().isClient
                && client.player != null
                && rocket.getWorld() == client.world) {
            ElytraHover.stopIfUnpowered(client.player);
        }
    }

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
