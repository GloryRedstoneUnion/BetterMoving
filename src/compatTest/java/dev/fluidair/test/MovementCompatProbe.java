package dev.fluidair.test;

import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.config.FluidMovementModel;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class MovementCompatProbe {
    private static final String WORLD_NAME = "Fluid Air Movement Compatibility Test";
    private static final double INITIAL_VELOCITY = 0.25;
    private static final double EXPECTED_WATER_HORIZONTAL_VELOCITY = 0.20;
    private static final double EXPECTED_SPRINTING_WATER_HORIZONTAL_VELOCITY = 0.225;
    private static final double EXPECTED_WATER_VERTICAL_VELOCITY = 0.195;
    private static final double TOLERANCE = 1.0E-6;
    private static final int TIMEOUT_TICKS = 1200;

    private State state = State.OPEN_WORLD_CREATION;
    private int ticks;

    public void tick(MinecraftClient client) {
        if (++this.ticks > TIMEOUT_TICKS) {
            throw new AssertionError("Timed out while preparing the movement compatibility test world");
        }

        if (this.state == State.OPEN_WORLD_CREATION
                && client.getOverlay() == null
                && client.currentScreen instanceof TitleScreen) {
            CreateWorldScreen.create(client, client.currentScreen);
            this.state = State.CREATE_WORLD;
            return;
        }

        if (this.state == State.CREATE_WORLD && client.currentScreen instanceof CreateWorldScreen screen) {
            screen.getWorldCreator().setWorldName(WORLD_NAME);
            screen.getWorldCreator().setGameMode(WorldCreator.Mode.CREATIVE);
            screen.getWorldCreator().setCheatsEnabled(true);
            screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
            this.state = State.RUN_TEST;
            return;
        }

        if (this.state == State.RUN_TEST && client.player != null && client.world != null) {
            verifyLavaUsesWaterMovement(client.player);
            this.state = State.COMPLETE;
            client.scheduleStop();
        }
    }

    private static void verifyLavaUsesWaterMovement(ClientPlayerEntity player) {
        FluidAirConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(true);
        FluidAirConfigs.MODEL.setOptionListValue(FluidMovementModel.WATER);

        BlockPos center = player.getBlockPos().up(2);
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 2, 2))) {
            player.getWorld().setBlockState(pos, Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
        }

        player.setPosition(Vec3d.ofCenter(center));
        player.setSwimming(false);
        player.setVelocity(Vec3d.ZERO);
        if (!player.updateMovementInFluid(FluidTags.LAVA, 0.0) || !player.isInLava()) {
            throw new AssertionError("The movement compatibility probe did not place the player in lava");
        }

        player.setSprinting(false);
        player.setVelocity(INITIAL_VELOCITY, 0.0, 0.0);
        player.travel(Vec3d.ZERO);
        assertVelocity(
                "horizontal water drag",
                EXPECTED_WATER_HORIZONTAL_VELOCITY,
                player.getVelocity().x);

        player.setPosition(Vec3d.ofCenter(center));
        player.setSprinting(true);
        player.setVelocity(INITIAL_VELOCITY, 0.0, 0.0);
        player.travel(Vec3d.ZERO);
        assertVelocity(
                "sprinting water drag",
                EXPECTED_SPRINTING_WATER_HORIZONTAL_VELOCITY,
                player.getVelocity().x);

        player.setPosition(Vec3d.ofCenter(center));
        player.setSprinting(false);
        player.setVelocity(0.0, INITIAL_VELOCITY, 0.0);
        player.travel(Vec3d.ZERO);
        assertVelocity(
                "vertical water drag and gravity",
                EXPECTED_WATER_VERTICAL_VELOCITY,
                player.getVelocity().y);
    }

    private static void assertVelocity(String behavior, double expected, double actual) {
        if (Math.abs(actual - expected) <= TOLERANCE) {
            return;
        }
        throw new AssertionError(
                "Lava used the wrong " + behavior + ": expected " + expected + " but got " + actual);
    }

    private enum State {
        OPEN_WORLD_CREATION,
        CREATE_WORLD,
        RUN_TEST,
        COMPLETE
    }
}
