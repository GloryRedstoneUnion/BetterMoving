package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.atomic.AtomicInteger;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import dev.bettermoving.entity.ClientFireworkRocketManager;
import dev.bettermoving.physics.ElytraFireworkPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    private static final AtomicInteger BETTERMOVING_LOCAL_ENTITY_ID = new AtomicInteger(Integer.MIN_VALUE);

    @Unique
    private boolean bettermoving$blockFireworkLaunchPacket;

    @Shadow
    public abstract ActionResult interactItem(PlayerEntity player, Hand hand);

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void bettermoving$redirectElytraFireworkBlockUse(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir) {
        this.bettermoving$blockFireworkLaunchPacket = false;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean holdingFirework = player.getStackInHand(hand).isOf(Items.FIREWORK_ROCKET);
        if (!ElytraFireworkPolicy.shouldRedirectBlockUse(
                BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.getBooleanValue(),
                client.player == player,
                player.isFallFlying(),
                holdingFirework)) {
            return;
        }

        cir.setReturnValue(this.interactItem(player, hand));
    }

    @Inject(
            method = "interactBlockInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;"),
            cancellable = true)
    private void bettermoving$blockNonElytraFireworkUse(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean holdingFirework = player.getStackInHand(hand).isOf(Items.FIREWORK_ROCKET);
        if (ElytraFireworkPolicy.shouldBlockNonElytraBlockUse(
                BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.getBooleanValue(),
                client.player == player,
                player.isFallFlying(),
                BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.getBooleanValue(),
                holdingFirework)) {
            this.bettermoving$blockFireworkLaunchPacket = true;
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @WrapOperation(
            method = "sendSequencedPacket",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void bettermoving$skipBlockedFireworkPacket(
            ClientPlayNetworkHandler networkHandler, Packet<?> packet, Operation<Void> original) {
        boolean blocked = this.bettermoving$blockFireworkLaunchPacket;
        this.bettermoving$blockFireworkLaunchPacket = false;
        // Skip only this packet, preserving the pending-update manager's cleanup.
        if (!blocked || !(packet instanceof PlayerInteractBlockC2SPacket)) {
            original.call(networkHandler, packet);
        }
    }

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
        ClientFireworkRocketManager.track(rocket);
        rocket.setId(BETTERMOVING_LOCAL_ENTITY_ID.getAndIncrement());
        clientWorld.addEntity(rocket.getId(), rocket);
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
