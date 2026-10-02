package dev.bettermoving.physics;

import dev.bettermoving.config.BetterMovingConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class LevitationElytraFlight {
    private static ClientPlayerEntity glidingPlayer;

    private LevitationElytraFlight() {
    }

    public static void start(PlayerEntity player) {
        if (player instanceof ClientPlayerEntity clientPlayer
                && clientPlayer == MinecraftClient.getInstance().player) {
            glidingPlayer = canContinue(clientPlayer) ? clientPlayer : null;
        }
    }

    public static void stop(PlayerEntity player) {
        if (glidingPlayer == player) {
            glidingPlayer = null;
        }
    }

    public static boolean resolve(LivingEntity entity, boolean trackedFlight) {
        if (!(entity instanceof ClientPlayerEntity player)
                || MinecraftClient.getInstance().player != player) {
            return trackedFlight;
        }
        if (!canContinue(player)) {
            glidingPlayer = null;
            return trackedFlight;
        }

        boolean levitating = player.hasStatusEffect(StatusEffects.LEVITATION);
        if (trackedFlight) {
            glidingPlayer = player;
        } else if (!levitating) {
            glidingPlayer = null;
        }
        // Vanilla servers reject Elytra flight during real Levitation. Keep the
        // local flight session without changing incoming metadata or server state.
        return trackedFlight || (levitating && glidingPlayer == player);
    }

    public static void tick(MinecraftClient client) {
        if (glidingPlayer != null
                && (glidingPlayer != client.player
                || glidingPlayer.getWorld() != client.world
                || !canContinue(glidingPlayer))) {
            glidingPlayer = null;
        }
    }

    private static boolean canContinue(ClientPlayerEntity player) {
        if (!BetterMovingConfigs.ignoreLevitationAndSlowness()
                || !player.isAlive()
                || player.isOnGround()
                || player.hasVehicle()
                || player.getAbilities().flying
                || player.isClimbing()) {
            return false;
        }
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(Items.ELYTRA) || !ElytraItem.isUsable(chest)) {
            return false;
        }
        FluidMovementContext.enter(player);
        try {
            return !player.isTouchingWater();
        } finally {
            FluidMovementContext.exit(player);
        }
    }
}
