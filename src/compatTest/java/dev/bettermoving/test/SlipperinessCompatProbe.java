package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.SlipperinessPolicy;
import fi.dy.masa.malilib.config.ConfigUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SlipperinessCompatProbe {
    private static final double TOLERANCE = 1.0E-6;

    private SlipperinessCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        Vec3d originalPosition = player.getPos();
        BlockPos floor = player.getBlockPos().withY(256);
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(false);
        try {
            verifyConfiguration();
            clearArea(client, floor);
            float stoneVelocity = measureVelocity(client, floor, Blocks.STONE, false);
            float iceVelocity = measureVelocity(client, floor, Blocks.ICE, false);
            check(iceVelocity > stoneVelocity + TOLERANCE,
                    "Vanilla ice did not retain higher slipperiness");

            float normalizedIceVelocity = measureVelocity(client, floor, Blocks.ICE, true);
            close("Enabled ice movement", stoneVelocity, normalizedIceVelocity);
            close("Enabled packed ice movement", stoneVelocity,
                    measureVelocity(client, floor, Blocks.PACKED_ICE, true));
            close("Enabled blue ice movement", stoneVelocity,
                    measureVelocity(client, floor, Blocks.BLUE_ICE, true));
            close("Normal block movement remains unchanged", stoneVelocity,
                    measureVelocity(client, floor, Blocks.STONE, true));
            BetterMovingClient.LOGGER.info("Slipperiness compatibility checks passed");
        } finally {
            BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(false);
            player.setPosition(originalPosition);
            player.setVelocity(Vec3d.ZERO);
            player.setOnGround(false);
            player.setNoGravity(false);
            player.baseTick();
        }
    }

    private static void verifyConfiguration() {
        check(!BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getDefaultBooleanValue(),
                "Slipperiness toggle must default to off");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS),
                "Missing slipperiness GUI toggle");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS),
                "Missing slipperiness toggle hotkey");
        check("".equals(BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getKeybind().getStringValue()),
                "Slipperiness toggle must default to unbound");
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getKeybind().setValueFromString("LEFT_ALT,S");
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(true);
        JsonObject serialized = new JsonObject();
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(false);
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getKeybind().setValueFromString("");
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getBooleanValue(),
                "Slipperiness toggle did not persist");
        check("LEFT_ALT,S".equals(BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getKeybind().getStringValue()),
                "Slipperiness hotkey did not persist");
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(false);
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getKeybind().setValueFromString("");
        check(SlipperinessPolicy.resolve(false, true, 0.98F) == 0.98F,
                "Disabled policy changed slipperiness");
        check(SlipperinessPolicy.resolve(true, false, 0.98F) == 0.98F,
                "Non-local policy changed slipperiness");
    }

    private static float measureVelocity(
            MinecraftClient client,
            BlockPos floor,
            Block floorBlock,
            boolean enabled) {
        clearArea(client, floor);
        client.world.setBlockState(floor.down(), floorBlock.getDefaultState(), Block.NOTIFY_LISTENERS);
        ClientPlayerEntity player = client.player;
        player.setPosition(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        player.setVelocity(0.4, 0.0, 0.0);
        player.setOnGround(true);
        player.setNoGravity(true);
        player.setSwimming(false);
        player.setSprinting(false);
        BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.setBooleanValue(enabled);
        player.travel(Vec3d.ZERO);
        return (float) player.getVelocity().x;
    }

    private static void clearArea(MinecraftClient client, BlockPos floor) {
        for (BlockPos pos : BlockPos.iterate(
                floor.add(-2, -2, -2), floor.add(2, 2, 2))) {
            client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void close(String label, float expected, float actual) {
        check(Math.abs(expected - actual) <= TOLERANCE,
                label + ": expected " + expected + " but got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
