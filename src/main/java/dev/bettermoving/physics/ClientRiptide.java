package dev.bettermoving.physics;

import dev.bettermoving.config.BetterMovingConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;

/** Owns a local use session so disabling the option cannot leak its release packet. */
public final class ClientRiptide {
    private static ClientPlayerEntity chargingPlayer;
    private static ItemStack chargingStack;
    private static ClientPlayerEntity spinningPlayer;

    private ClientRiptide() {
    }

    public static boolean shouldSimulate(PlayerEntity player, ItemStack stack) {
        return BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.getBooleanValue()
                && player == MinecraftClient.getInstance().player
                && stack.isOf(Items.TRIDENT)
                && EnchantmentHelper.getRiptide(stack) > 0;
    }

    public static ActionResult start(ClientPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!isEligiblePlayer(player)
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return ActionResult.PASS;
        }
        TypedActionResult<ItemStack> result = stack.use(player.getWorld(), player, hand);
        if (result.getResult().isAccepted() && player.isUsingItem()
                && player.getActiveItem() == stack) {
            chargingPlayer = player;
            chargingStack = stack;
        }
        return result.getResult();
    }

    public static boolean isCharging(LivingEntity player) {
        return chargingPlayer == player;
    }

    public static void release(PlayerEntity player) {
        if (canContinueCharging()) {
            // Vanilla already computes the impulse, ground lift, and 20-tick spin.
            player.stopUsingItem();
        } else {
            player.clearActiveItem();
        }
        clearCharge(player);
    }

    public static void clearCharge(LivingEntity player) {
        if (chargingPlayer == player) {
            chargingPlayer = null;
            chargingStack = null;
        }
    }

    public static void startSpin(PlayerEntity player) {
        if (isCharging(player)) {
            spinningPlayer = chargingPlayer;
        } else if (spinningPlayer == player) {
            spinningPlayer = null;
        }
    }

    public static boolean isSpinning(LivingEntity player) {
        return spinningPlayer == player
                && player == MinecraftClient.getInstance().player
                && player.isAlive()
                && BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.getBooleanValue();
    }

    public static void tick(MinecraftClient client) {
        if (chargingPlayer != null && !canContinueCharging()) {
            ClientPlayerEntity player = chargingPlayer;
            clearCharge(player);
            player.clearActiveItem();
        }
        if (spinningPlayer != null && (!isSpinning(spinningPlayer)
                || spinningPlayer.getWorld() != client.world)) {
            spinningPlayer = null;
        }
    }

    private static boolean canContinueCharging() {
        return chargingPlayer != null
                && isEligiblePlayer(chargingPlayer)
                && chargingPlayer.isUsingItem()
                && chargingPlayer.getActiveItem() == chargingStack
                && chargingPlayer.getStackInHand(chargingPlayer.getActiveHand()) == chargingStack
                && shouldSimulate(chargingPlayer, chargingStack);
    }

    private static boolean isEligiblePlayer(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return player == client.player
                && player.getWorld() == client.world
                && client.interactionManager != null
                // Match the interaction manager's vanilla spectator gate even
                // before the player-list entry receives its game-mode update.
                && client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR
                && !player.isSpectator()
                && player.isAlive();
    }
}
