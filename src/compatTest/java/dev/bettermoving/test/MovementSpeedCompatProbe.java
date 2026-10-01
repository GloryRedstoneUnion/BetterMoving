package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.MovementSpeedPolicy;
import fi.dy.masa.malilib.config.ConfigUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class MovementSpeedCompatProbe {
    private static final double TOLERANCE = 1.0E-8;

    private MovementSpeedCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        Vec3d originalPosition = player.getPos();
        Vec3d originalVelocity = player.getVelocity();
        boolean originalOnGround = player.isOnGround();
        boolean originalNoGravity = player.hasNoGravity();
        boolean originalSwimming = player.isSwimming();
        boolean originalSprinting = player.isSprinting();
        BlockPos floor = player.getBlockPos().withY(256);

        try {
            verifyConfiguration();
            clearArea(client, floor);
            client.world.setBlockState(floor.down(), Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);

            Vec3d baseline = measureFinalVelocity(client, floor, 0.0);
            Vec3d boosted = measureFinalVelocity(client, floor, 0.5);
            close("Boosted X velocity", baseline.x * 1.5, boosted.x);
            close("Boosted Y velocity", baseline.y * 1.5, boosted.y);
            close("Boosted Z velocity", baseline.z * 1.5, boosted.z);
            close("Policy X velocity", boosted.x, MovementSpeedPolicy.apply(baseline, 0.5).x);
            BetterMovingClient.LOGGER.info("Movement speed compatibility checks passed");
        } finally {
            BetterMovingConfigs.MOVEMENT_SPEED_BOOST.setDoubleValue(0.0);
            player.setPosition(originalPosition);
            player.setVelocity(originalVelocity);
            player.setOnGround(originalOnGround);
            player.setNoGravity(originalNoGravity);
            player.setSwimming(originalSwimming);
            player.setSprinting(originalSprinting);
            player.baseTick();
        }
    }

    private static void verifyConfiguration() {
        check(BetterMovingConfigs.MOVEMENT_SPEED_BOOST.getDefaultDoubleValue() == 0.0,
                "Movement speed boost must default to zero");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.MOVEMENT_SPEED_BOOST),
                "Missing movement speed boost GUI option");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.MOVEMENT_SPEED_BOOST),
                "Movement speed boost must not be registered as a hotkey");

        BetterMovingConfigs.MOVEMENT_SPEED_BOOST.setDoubleValue(0.5);
        JsonObject serialized = new JsonObject();
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.MOVEMENT_SPEED_BOOST.setDoubleValue(0.0);
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        close("Persisted movement speed boost", 0.5,
                BetterMovingConfigs.MOVEMENT_SPEED_BOOST.getDoubleValue());
        BetterMovingConfigs.MOVEMENT_SPEED_BOOST.setDoubleValue(0.0);
    }

    private static Vec3d measureFinalVelocity(
            MinecraftClient client,
            BlockPos floor,
            double boost) {
        ClientPlayerEntity player = client.player;
        player.setPosition(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        player.setVelocity(0.2, -0.1, 0.15);
        player.setOnGround(true);
        player.setNoGravity(true);
        player.setSwimming(false);
        player.setSprinting(false);
        BetterMovingConfigs.MOVEMENT_SPEED_BOOST.setDoubleValue(boost);
        player.tickMovement();
        return player.getVelocity();
    }

    private static void clearArea(MinecraftClient client, BlockPos floor) {
        for (BlockPos pos : BlockPos.iterate(
                floor.add(-2, -2, -2), floor.add(2, 2, 2))) {
            client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void close(String label, double expected, double actual) {
        check(Math.abs(expected - actual) <= TOLERANCE,
                label + ": expected " + expected + " but got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
