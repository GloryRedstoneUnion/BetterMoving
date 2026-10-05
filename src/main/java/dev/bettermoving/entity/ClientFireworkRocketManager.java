package dev.bettermoving.entity;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.ElytraFireworkPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public final class ClientFireworkRocketManager {
    private static final Set<FireworkRocketEntity> LOCAL_SIMULATIONS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ClientFireworkRocketManager() {
    }

    public static void track(FireworkRocketEntity rocket) {
        LOCAL_SIMULATIONS.add(rocket);
    }

    public static void cancelOnGlideStop(ClientPlayerEntity player, boolean endedActiveGlide) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!endedActiveGlide
                || client.world == null
                || !ElytraFireworkPolicy.shouldCancelSimulatedFireworkOnGlideStop(
                        BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.getBooleanValue(),
                        BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue(),
                        client.player == player)) {
            return;
        }

        Iterator<FireworkRocketEntity> iterator = LOCAL_SIMULATIONS.iterator();
        while (iterator.hasNext()) {
            FireworkRocketEntity rocket = iterator.next();
            if (rocket.isRemoved()
                    || rocket.getWorld() != client.world
                    || rocket.getOwner() != player) {
                iterator.remove();
                continue;
            }
            ((ClientFireworkRocket) rocket).bettermoving$discardIfLocalSimulation();
            iterator.remove();
        }
    }
}
