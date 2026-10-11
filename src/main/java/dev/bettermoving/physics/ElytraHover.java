package dev.bettermoving.physics;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocketManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/** Stops unpowered local gliding without ending the flight or changing position. */
public final class ElytraHover {
    private ElytraHover() {
    }

    public static boolean shouldHover(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return BetterMovingConfigs.HOVER_WHEN_ELYTRA_UNPOWERED.getBooleanValue()
                && entity instanceof ClientPlayerEntity player
                && player == client.player
                && player.getWorld() == client.world
                && player.isAlive()
                && !player.isOnGround()
                && !player.hasVehicle()
                && !player.getAbilities().flying
                && !player.isSpectator()
                && player.isFallFlying()
                && !((ElytraBoostState) player).bettermoving$hasRiptidePropulsion()
                && !ClientFireworkRocketManager.hasActiveBoost(player);
    }

    public static void stopIfUnpowered(LivingEntity entity) {
        if (shouldHover(entity)) {
            entity.setVelocity(Vec3d.ZERO);
        }
    }
}
