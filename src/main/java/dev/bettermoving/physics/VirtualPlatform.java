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

public final class VirtualPlatform {
    private static ClientPlayerEntity anchoredPlayer;
    private static World anchoredWorld;
    private static double height;

    private VirtualPlatform() {
    }

    public static void onOptionChanged() {
        clear();
        tick(MinecraftClient.getInstance());
    }

    public static void tick(MinecraftClient client) {
        if (!BetterMovingConfigs.VIRTUAL_PLATFORM.getBooleanValue()
                || client.player == null
                || client.world == null) {
            clear();
            return;
        }
        if (anchoredPlayer != client.player || anchoredWorld != client.player.getWorld()) {
            anchoredPlayer = client.player;
            anchoredWorld = client.player.getWorld();
            height = client.player.getBoundingBox().minY;
        }
    }

    public static List<VoxelShape> addMovementCollision(
            Entity entity, Vec3d movement, List<VoxelShape> collisions) {
        if (!isActiveFor(entity)) {
            return collisions;
        }
        // Cover the swept area and vanilla step-up candidates without creating world blocks.
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

    /**
     * Returns whether the entity is standing on this platform without a real block below it.
     * Movement still uses the virtual collision, but mining must retain vanilla air penalties.
     */
    public static boolean isVirtualOnlyMiningSupport(Entity entity) {
        if (!isActiveFor(entity) || !entity.isOnGround()) {
            return false;
        }
        Box feet = entity.getBoundingBox();
        Box supportProbe = new Box(
                feet.minX,
                feet.minY - 1.0E-3,
                feet.minZ,
                feet.maxX,
                feet.minY + 1.0E-3,
                feet.maxZ);
        return !entity.getWorld().getBlockCollisions(entity, supportProbe).iterator().hasNext();
    }

    private static boolean isActiveFor(Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity != client.player || entity.hasVehicle()) {
            return false;
        }
        tick(client);
        return anchoredPlayer == entity && anchoredWorld == entity.getWorld();
    }

    private static void clear() {
        anchoredPlayer = null;
        anchoredWorld = null;
    }
}
