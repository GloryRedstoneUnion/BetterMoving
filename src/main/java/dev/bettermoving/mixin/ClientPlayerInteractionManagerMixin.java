package dev.bettermoving.mixin;

import java.util.concurrent.atomic.AtomicInteger;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import dev.bettermoving.physics.ElytraFireworkPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    private static final AtomicInteger BETTERMOVING_LOCAL_ENTITY_ID = new AtomicInteger(Integer.MIN_VALUE);

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void bettermoving$simulateElytraFirework(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld clientWorld = client.world;
        if (!(player instanceof ClientPlayerEntity clientPlayer)
                || client.player != clientPlayer
                || clientWorld == null) {
            return;
        }

        boolean holdingFirework = clientPlayer.getStackInHand(hand).isOf(Items.FIREWORK_ROCKET);
        if (!ElytraFireworkPolicy.shouldSimulate(
                BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue(),
                true,
                clientPlayer.isFallFlying(),
                holdingFirework)) {
            return;
        }

        FireworkRocketEntity rocket = new FireworkRocketEntity(
                clientWorld,
                clientPlayer.getStackInHand(hand).copy(),
                clientPlayer);
        ((ClientFireworkRocket) rocket).bettermoving$markLocalSimulation();
        rocket.setId(BETTERMOVING_LOCAL_ENTITY_ID.getAndIncrement());
        clientWorld.addEntity(rocket.getId(), rocket);
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
