package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.config.FluidMovementModel;
import dev.bettermoving.physics.VirtualPlatform;
import fi.dy.masa.malilib.config.ConfigUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class VirtualPlatformCompatProbe {
    private static final double TOLERANCE = 1.0E-6;

    private VirtualPlatformCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        Vec3d originalPosition = player.getPos();
        boolean allowedFlying = player.getAbilities().allowFlying;
        BlockPos floor = player.getBlockPos().withY(256);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
        player.getAbilities().allowFlying = false;
        try {
            verifyConfiguration();
            clearArea(client, floor);
            verifyFixedHeightAndIsolation(client, floor);
            for (Motion motion : Motion.values()) {
                List<Sample> ground = measure(client, floor, false, motion);
                List<Sample> platform = measure(client, floor, true, motion);
                for (int tick = 0; tick < ground.size(); ++tick) {
                    compareSamples(motion + " tick " + tick, ground.get(tick), platform.get(tick));
                }
                BetterMovingClient.LOGGER.info("Virtual platform matched stone ground: {}", motion);
            }
            verifyObstacles(client, floor);
            verifyFluidModes(client, floor);
            verifyToggleAndSessionReset(client, floor);
            BetterMovingClient.LOGGER.info("Virtual platform compatibility checks passed");
        } finally {
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
            releaseKeys(client);
            player.getAbilities().allowFlying = allowedFlying;
            player.setNoGravity(false);
            player.setPosition(originalPosition);
            player.setVelocity(Vec3d.ZERO);
            player.setOnGround(false);
            player.setSneaking(false);
            player.setSprinting(false);
            player.setSwimming(false);
            player.baseTick();
        }
    }

    private static void verifyConfiguration() {
        check(!BetterMovingConfigs.VIRTUAL_PLATFORM.getDefaultBooleanValue(), "Platform must default to off");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.VIRTUAL_PLATFORM), "Missing GUI toggle");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.VIRTUAL_PLATFORM), "Missing toggle hotkey");
        BetterMovingConfigs.VIRTUAL_PLATFORM.getKeybind().setValueFromString("LEFT_ALT,P");
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
        JsonObject serialized = new JsonObject();
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        BetterMovingConfigs.VIRTUAL_PLATFORM.getKeybind().setValueFromString("");
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.VIRTUAL_PLATFORM.getBooleanValue(), "Platform toggle did not persist");
        check("LEFT_ALT,P".equals(BetterMovingConfigs.VIRTUAL_PLATFORM.getKeybind().getStringValue()),
                "Platform hotkey did not persist");
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        BetterMovingConfigs.VIRTUAL_PLATFORM.getKeybind().setValueFromString("");
    }

    private static void verifyFixedHeightAndIsolation(MinecraftClient client, BlockPos floor) {
        ClientPlayerEntity player = client.player;
        resetPlayer(client, floor);
        player.move(MovementType.SELF, new Vec3d(0.0, -0.25, 0.0));
        close("Disabled platform allows falling", floor.getY() - 0.25, player.getY());

        resetPlayer(client, floor);
        double height = floor.getY() + 0.375;
        player.setPosition(player.getX(), height, player.getZ());
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
        player.move(MovementType.SELF, new Vec3d(0.0, -0.25, 0.0));
        close("Exact captured feet height", height, player.getY());
        check(player.isOnGround() && player.verticalCollision, "Platform must cause ordinary ground collision");
        check(!player.getAbilities().flying && !player.hasNoGravity(), "Platform must not enable flight");
        check(client.world.getBlockState(player.getBlockPos().down()).isAir(), "Platform created a world block");
        check(client.world.isSpaceEmpty(player, player.getBoundingBox().offset(0.0, -0.5, 0.0)),
                "Platform leaked into ordinary world collision queries");
        HitResult hit = client.world.raycast(new RaycastContext(
                player.getPos().add(0.0, 1.0, 0.0), player.getPos().add(0.0, -2.0, 0.0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        check(hit.getType() == HitResult.Type.MISS, "Platform became an interaction target");

        ArmorStandEntity other = new ArmorStandEntity(client.world, player.getX(), height, player.getZ());
        other.move(MovementType.SELF, new Vec3d(0.0, -0.25, 0.0));
        close("Other entities remain unaffected", height - 0.25, other.getY());

        player.move(MovementType.SELF, new Vec3d(0.0, 1.0, 0.0));
        close("Platform permits upward movement", height + 1.0, player.getY());
        player.move(MovementType.SELF, new Vec3d(2.0, -2.0, 2.0));
        close("Jump returns to fixed platform height", height, player.getY());
        Vec3d before = player.getPos();
        player.move(MovementType.SELF, new Vec3d(24.0, -100.0, 24.0));
        close("Fast diagonal motion X", before.x + 24.0, player.getX());
        close("Fast diagonal motion Z", before.z + 24.0, player.getZ());
        close("Fast downward collision", height, player.getY());
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
    }

    private static List<Sample> measure(
            MinecraftClient client, BlockPos floor, boolean virtual, Motion motion) {
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        setFloor(client, floor, !virtual);
        resetPlayer(client, floor);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(virtual);
        ClientPlayerEntity player = client.player;
        player.move(MovementType.SELF, new Vec3d(0.0, -0.1, 0.0));
        check(player.isOnGround(), "Test surface did not support the player");
        // Let vanilla establish its normal standing velocity before recording movement.
        for (int tick = 0; tick < 2; ++tick) {
            player.baseTick();
            player.tickMovement();
        }
        Vec3d start = player.getPos();
        List<Sample> samples = new ArrayList<>();
        for (int tick = 0; tick < 32; ++tick) {
            client.options.forwardKey.setPressed(true);
            client.options.sprintKey.setPressed(motion == Motion.SPRINT || motion == Motion.SPRINT_JUMP);
            client.options.sneakKey.setPressed(motion == Motion.SNEAK);
            client.options.jumpKey.setPressed(motion == Motion.SPRINT_JUMP && tick == 0);
            player.setSprinting(motion == Motion.SPRINT || motion == Motion.SPRINT_JUMP);
            player.baseTick();
            player.tickMovement();
            samples.add(new Sample(player.getPos().subtract(start), player.getVelocity(), player.isOnGround()));
        }
        releaseKeys(client);
        check(samples.get(31).position.x > 0.1, "Player did not move on the " + motion + " surface");
        if (motion == Motion.SPRINT_JUMP) {
            check(samples.stream().anyMatch(sample -> sample.position.y > 1.0 && !sample.onGround),
                    "Sprint jump did not leave the surface");
            check(samples.get(31).onGround, "Sprint jump did not land back on the surface");
        } else {
            check(samples.stream().allMatch(Sample::onGround),
                    motion + " lost ground contact on " + (virtual ? "virtual platform" : "stone ground"));
        }
        return samples;
    }

    private static void verifyObstacles(MinecraftClient client, BlockPos floor) {
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        setFloor(client, floor, false);
        resetPlayer(client, floor);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
        ClientPlayerEntity player = client.player;
        for (int y = 0; y < 3; ++y) {
            client.world.setBlockState(floor.east().up(y), Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        player.move(MovementType.SELF, new Vec3d(2.0, -0.1, 0.0));
        check(player.getX() < floor.getX() + 1.0 && player.horizontalCollision, "Real wall collision was lost");
        for (int y = 0; y < 3; ++y) {
            client.world.setBlockState(floor.east().up(y), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        resetPlayer(client, floor);
        client.world.setBlockState(floor.up(2), Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
        player.move(MovementType.SELF, new Vec3d(0.0, 1.0, 0.0));
        check(player.getY() < floor.getY() + 0.3 && player.verticalCollision, "Real ceiling collision was lost");
        client.world.setBlockState(floor.up(2), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        resetPlayer(client, floor);
        client.world.setBlockState(floor.east(), Blocks.STONE_SLAB.getDefaultState(), Block.NOTIFY_LISTENERS);
        player.move(MovementType.SELF, new Vec3d(1.0, -0.1, 0.0));
        close("Step up on a real slab", floor.getY() + 0.5, player.getY());
        player.move(MovementType.SELF, new Vec3d(1.0, 0.0, 0.0));
        player.move(MovementType.SELF, new Vec3d(0.0, -1.0, 0.0));
        close("Return from real step to virtual floor", floor.getY(), player.getY());
        client.world.setBlockState(floor.east(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static void verifyFluidModes(MinecraftClient client, BlockPos floor) {
        for (FluidMovementModel model : FluidMovementModel.values()) {
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
            resetPlayer(client, floor);
            for (BlockPos pos : BlockPos.iterate(floor.add(-1, 0, -1), floor.add(1, 2, 1))) {
                client.world.setBlockState(pos, Blocks.LAVA.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(true);
            BetterMovingConfigs.MODEL.setOptionListValue(model);
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
            client.player.baseTick();
            client.player.travel(Vec3d.ZERO);
            client.player.travel(Vec3d.ZERO);
            close("Platform support with " + model + " in lava", floor.getY(), client.player.getY());
            check(!client.player.getAbilities().flying, "Fluid combination enabled flight");
        }
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
        for (BlockPos pos : BlockPos.iterate(floor.add(-1, 0, -1), floor.add(1, 2, 1))) {
            client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void verifyToggleAndSessionReset(MinecraftClient client, BlockPos floor) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        resetPlayer(client, floor);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        player.move(MovementType.SELF, new Vec3d(0.0, -0.5, 0.0));
        close("Disabling restores falling immediately", floor.getY() - 0.5, player.getY());
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(true);
        player.move(MovementType.SELF, new Vec3d(0.0, -0.5, 0.0));
        close("Re-enabling captures the new height", floor.getY() - 0.5, player.getY());

        client.player = null;
        try {
            VirtualPlatform.tick(client);
        } finally {
            client.player = player;
        }
        player.setPosition(player.getX(), floor.getY() + 2.25, player.getZ());
        VirtualPlatform.tick(client);
        player.move(MovementType.SELF, new Vec3d(0.0, -1.0, 0.0));
        close("New player session captures a fresh height", floor.getY() + 2.25, player.getY());
    }

    private static void resetPlayer(MinecraftClient client, BlockPos floor) {
        releaseKeys(client);
        ClientPlayerEntity player = client.player;
        player.setPosition(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        player.setVelocity(Vec3d.ZERO);
        player.setYaw(-90.0F);
        player.setPitch(0.0F);
        player.setOnGround(false);
        player.setNoGravity(false);
        player.setSwimming(false);
        player.setSneaking(false);
        player.setSprinting(false);
        player.setPose(EntityPose.STANDING);
        player.getAbilities().flying = false;
        player.fallDistance = 0.0F;
        player.baseTick();
    }

    private static void releaseKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
    }

    private static void clearArea(MinecraftClient client, BlockPos floor) {
        for (BlockPos pos : BlockPos.iterate(floor.add(-16, -3, -4), floor.add(16, 5, 4))) {
            client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void setFloor(MinecraftClient client, BlockPos floor, boolean solid) {
        for (BlockPos pos : BlockPos.iterate(floor.add(-16, -1, -4), floor.add(16, -1, 4))) {
            client.world.setBlockState(pos, (solid ? Blocks.STONE : Blocks.AIR).getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void compareSamples(String label, Sample expected, Sample actual) {
        close(label + " X", expected.position.x, actual.position.x);
        close(label + " Y", expected.position.y, actual.position.y);
        close(label + " Z", expected.position.z, actual.position.z);
        close(label + " velocity X", expected.velocity.x, actual.velocity.x);
        close(label + " velocity Y", expected.velocity.y, actual.velocity.y);
        close(label + " velocity Z", expected.velocity.z, actual.velocity.z);
        check(expected.onGround == actual.onGround, label + " ground state mismatch");
    }

    private static void close(String label, double expected, double actual) {
        check(Math.abs(expected - actual) <= TOLERANCE, label + ": expected " + expected + " but got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private enum Motion {
        WALK, SPRINT, SNEAK, SPRINT_JUMP
    }

    private record Sample(Vec3d position, Vec3d velocity, boolean onGround) {
    }
}
