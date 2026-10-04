package dev.bettermoving.physics;

import dev.bettermoving.config.BetterMovingConfigs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * Adds a movement-only floor at the active world's minimum build height.
 */
public final class VoidProtectionPlatform {
    private static ClientPlayerEntity activePlayer;
    private static World activeWorld;
    private static int height;

    private VoidProtectionPlatform() {
    }

    public static void onOptionChanged() {
        clear();
        tick(MinecraftClient.getInstance());
    }

    public static void tick(MinecraftClient client) {
        if (!BetterMovingConfigs.VOID_PROTECTION_PLATFORM.getBooleanValue()
                || client.player == null
                || client.world == null) {
            clear();
            return;
        }
        if (activePlayer != client.player || activeWorld != client.player.getWorld()) {
            activePlayer = client.player;
            activeWorld = client.player.getWorld();
            height = activeWorld.getBottomY();
        }
    }

    public static List<VoxelShape> addMovementCollision(
            Entity entity, Vec3d movement, List<VoxelShape> collisions) {
        if (!isActiveFor(entity)) {
            return collisions;
        }
        Box sweep = entity.getBoundingBox().stretch(movement).expand(1.0);
        VoxelShape platform = VoxelShapes.cuboid(
                sweep.minX, height - 1.0, sweep.minZ,
                sweep.maxX, height, sweep.maxZ);
        List<VoxelShape> result = new ArrayList<>(collisions.size() + 1);
        result.addAll(collisions);
        result.add(platform);
        return result;
    }

    public static boolean intersectsMovementSupport(Entity entity, Box box) {
        return isActiveFor(entity) && box.minY < height && box.maxY > height - 1.0;
    }

    private static boolean isActiveFor(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity != client.player || entity.hasVehicle()) {
            return false;
        }
        tick(client);
        return activePlayer == entity && activeWorld == entity.getWorld();
    }

    private static void clear() {
        activePlayer = null;
        activeWorld = null;
    }
}
