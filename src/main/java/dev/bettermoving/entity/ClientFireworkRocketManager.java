package dev.bettermoving.entity;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.ElytraFireworkPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class ClientFireworkRocketManager {
    // Entity IDs (and hash codes) change when a rocket is added to the world.
    // Weak references compared by identity keep registration valid across that change.
    private static final List<WeakReference<FireworkRocketEntity>> TRACKED_ROCKETS = new ArrayList<>();

    private ClientFireworkRocketManager() {
    }

    public static void track(FireworkRocketEntity rocket) {
        if (!rocket.getWorld().isClient) {
            return;
        }
        TRACKED_ROCKETS.removeIf(reference -> reference.get() == null);
        if (TRACKED_ROCKETS.stream().noneMatch(reference -> reference.get() == rocket)) {
            TRACKED_ROCKETS.add(new WeakReference<>(rocket));
        }
    }

    public static boolean hasActiveBoost(ClientPlayerEntity player) {
        // Constructors register both server-spawned and simulated rockets, so
        // the first player travel after a rocket appears can already resume.
        TRACKED_ROCKETS.removeIf(reference -> {
            FireworkRocketEntity rocket = reference.get();
            return rocket == null || rocket.isRemoved() || rocket.getWorld() != player.getWorld();
        });
        return TRACKED_ROCKETS.stream().anyMatch(reference -> {
            FireworkRocketEntity rocket = reference.get();
            return rocket != null && ((ClientFireworkRocket) rocket).bettermoving$isBoosting(player);
        });
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

        // Discarding a rocket can immediately check the remaining propulsion.
        // Iterate a snapshot so that check can prune the live registry safely.
        for (WeakReference<FireworkRocketEntity> reference : List.copyOf(TRACKED_ROCKETS)) {
            FireworkRocketEntity rocket = reference.get();
            if (rocket == null || rocket.isRemoved()
                    || rocket.getWorld() != client.world) {
                TRACKED_ROCKETS.remove(reference);
                continue;
            }
            if (rocket.getOwner() == player) {
                ((ClientFireworkRocket) rocket).bettermoving$discardIfLocalSimulation();
                if (rocket.isRemoved()) {
                    TRACKED_ROCKETS.remove(reference);
                }
            }
        }
    }
}
