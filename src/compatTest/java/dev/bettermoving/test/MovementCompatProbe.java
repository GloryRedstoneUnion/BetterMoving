package dev.bettermoving.test;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.config.FluidMovementModel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class MovementCompatProbe {
    private static final String WORLD_NAME = "BetterMoving Movement Compatibility Test";
    private static final double INITIAL_VELOCITY = 0.25;
    private static final double EXPECTED_WATER_HORIZONTAL_VELOCITY = 0.20;
    private static final double EXPECTED_SPRINTING_WATER_HORIZONTAL_VELOCITY = 0.225;
    private static final double EXPECTED_WATER_VERTICAL_VELOCITY = 0.195;
    private static final double EXPECTED_SWIMMING_INPUT_DISTANCE = 0.0196;
    private static final double SWIMMING_TOLERANCE = 1.0E-5;
    private static final double TOLERANCE = 1.0E-6;
    private static final int SUSTAINED_SWIMMING_TICKS = 20;
    private static final int TIMEOUT_TICKS = 1200;

    private static boolean captureMovementWaterState;
    private static boolean movementWaterStateObserved;
    private static boolean movementTouchingWater;
    private static boolean movementSubmergedInWater;
    private static boolean movementInLava;
    private static boolean movementSwimming;
    private static double movementWaterHeight;
    private static double movementLavaHeight;

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

        if (client.player == null || client.world == null) {
            return;
        }

        if (this.state == State.RUN_TEST) {
            if (client.player.age < 5) {
                return;
            }
            if (Boolean.getBoolean("bettermoving.momentumCompatTest")) {
                MomentumResetCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.potionEffectsCompatTest")) {
                PotionEffectsCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.elytraFireworksCompatTest")) {
                ElytraFireworkCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.riptideCompatTest")) {
                RiptideCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.sprintCompatTest")) {
                SprintHungerCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.slipperinessCompatTest")) {
                SlipperinessCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.platformCompatTest")) {
                VirtualPlatformCompatProbe.verify(client);
            }
            if (Boolean.getBoolean("bettermoving.movementCompatTest")) {
                verifyDirectLavaMovement(client);
                verifySwimmingInputMatchesWater(client);
                verifyScopedAirState(client);
            }
            if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                ModMenuCompatProbe.openConfigScreen(client);
            }
            this.state = State.COMPLETE;
            client.scheduleStop();
        }
    }

    private static void verifyDirectLavaMovement(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(true);
        BetterMovingConfigs.MODEL.setOptionListValue(FluidMovementModel.WATER);

        BlockPos center = player.getBlockPos().up(2);
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 2, 2))) {
            player.getWorld().setBlockState(pos, Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
        }

        player.setPosition(Vec3d.ofCenter(center));
        player.setSwimming(false);
        player.setVelocity(Vec3d.ZERO);
        boolean movementInLava = player.updateMovementInFluid(FluidTags.LAVA, 0.0);
        if (!movementInLava || !player.isInLava()) {
            throw new AssertionError(
                    "The movement compatibility probe did not place the player in lava: "
                            + "centerFluid=" + player.getWorld().getFluidState(center)
                            + ", movementInLava=" + movementInLava
                            + ", isInLava=" + player.isInLava()
                            + ", playerPos=" + player.getPos());
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

    private static void verifySwimmingInputMatchesWater(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos waterCenter = player.getBlockPos().add(16, 4, 0);
        double waterDistance = measureSwimmingInputDistance(
                client,
                waterCenter,
                Blocks.WATER,
                FluidTags.WATER);
        double lavaDistance = measureSwimmingInputDistance(
                client,
                waterCenter.add(16, 0, 0),
                Blocks.LAVA,
                FluidTags.LAVA);
        assertClose(
                "Water swimming input used the wrong horizontal distance",
                EXPECTED_SWIMMING_INPUT_DISTANCE,
                waterDistance,
                SWIMMING_TOLERANCE);
        assertClose(
                "Lava swimming input did not match water",
                waterDistance,
                lavaDistance,
                SWIMMING_TOLERANCE);

        double sustainedWaterDistance = measureSwimmingInputDistance(
                client,
                waterCenter.add(32, 0, 0),
                Blocks.WATER,
                FluidTags.WATER,
                SUSTAINED_SWIMMING_TICKS);
        double sustainedLavaDistance = measureSwimmingInputDistance(
                client,
                waterCenter.add(48, 0, 0),
                Blocks.LAVA,
                FluidTags.LAVA,
                SUSTAINED_SWIMMING_TICKS);
        assertClose(
                "Sustained lava swimming did not match water",
                sustainedWaterDistance,
                sustainedLavaDistance,
                SWIMMING_TOLERANCE);
    }

    private static double measureSwimmingInputDistance(
            MinecraftClient client,
            BlockPos center,
            Block fluidBlock,
            TagKey<Fluid> fluidTag) {
        return measureSwimmingInputDistance(client, center, fluidBlock, fluidTag, 1);
    }

    private static double measureSwimmingInputDistance(
            MinecraftClient client,
            BlockPos center,
            Block fluidBlock,
            TagKey<Fluid> fluidTag,
            int movementTicks) {
        ClientPlayerEntity player = client.player;
        for (BlockPos pos : BlockPos.iterate(center.add(-4, -2, -4), center.add(4, 2, 4))) {
            client.world.setBlockState(pos, fluidBlock.getDefaultState(), Block.NOTIFY_ALL);
        }

        player.setPosition(Vec3d.ofCenter(center));
        player.setYaw(-90.0F);
        player.setPitch(0.0F);
        player.setVelocity(Vec3d.ZERO);
        player.setOnGround(false);
        player.setNoGravity(true);
        player.setSwimming(false);
        player.setSprinting(true);
        player.baseTick();
        if (!player.isSubmergedIn(fluidTag)) {
            throw new AssertionError("The swimming movement probe did not place the player in " + fluidTag.id());
        }

        boolean lavaProbe = fluidTag == FluidTags.LAVA;
        if (lavaProbe) {
            assertRealLavaStateOutsideMovement(player);
        }
        resetMovementStateCapture();
        captureMovementWaterState = true;

        Vec3d movementStart = player.getPos();
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        try {
            player.setSprinting(true);
            for (int i = 0; i < movementTicks; ++i) {
                player.baseTick();
                player.tickMovement();
            }
        } finally {
            captureMovementWaterState = false;
            stopMovementInput(client);
        }
        if (lavaProbe) {
            if (!movementWaterStateObserved) {
                throw new AssertionError("The movement water-state probe did not run");
            }
            if (!movementTouchingWater
                    || !movementSubmergedInWater
                    || movementInLava
                    || !movementSwimming
                    || movementWaterHeight <= 0.0
                    || movementLavaHeight != 0.0) {
                throw new AssertionError(
                        "Lava was not exposed as a complete water state inside local movement: touchingWater="
                                + movementTouchingWater
                                + ", submergedInWater="
                                + movementSubmergedInWater
                                + ", isInLava="
                                + movementInLava
                                + ", swimming="
                                + movementSwimming
                                + ", waterHeight="
                                + movementWaterHeight
                                + ", lavaHeight="
                                + movementLavaHeight);
            }
            assertRealLavaStateOutsideMovement(player);
        }
        return player.getPos().subtract(movementStart).horizontalLength();
    }

    public static void observeMovementWaterState(ClientPlayerEntity player) {
        if (!captureMovementWaterState) {
            return;
        }
        movementWaterStateObserved = true;
        movementTouchingWater = player.isTouchingWater();
        movementSubmergedInWater = player.isSubmergedInWater();
        movementInLava = player.isInLava();
        movementSwimming = player.isSwimming();
        movementWaterHeight = player.getFluidHeight(FluidTags.WATER);
        movementLavaHeight = player.getFluidHeight(FluidTags.LAVA);
    }

    private static void assertRealLavaStateOutsideMovement(ClientPlayerEntity player) {
        if (!player.isInLava() || player.isTouchingWater() || player.isSubmergedInWater()) {
            throw new AssertionError(
                    "Lava state was not preserved outside local movement: isInLava="
                            + player.isInLava()
                            + ", touchingWater="
                            + player.isTouchingWater()
                            + ", submergedInWater="
                            + player.isSubmergedInWater());
        }
    }

    private static void verifyScopedAirState(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.MODEL.setOptionListValue(FluidMovementModel.AIR);
        BlockPos center = player.getBlockPos().add(64, 4, 0);
        for (BlockPos pos : BlockPos.iterate(center.add(-4, -2, -4), center.add(4, 2, 4))) {
            client.world.setBlockState(pos, Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
        }
        player.setPosition(Vec3d.ofCenter(center));
        player.setVelocity(Vec3d.ZERO);
        player.setNoGravity(true);
        player.baseTick();
        assertRealLavaStateOutsideMovement(player);

        resetMovementStateCapture();
        captureMovementWaterState = true;
        try {
            player.tickMovement();
        } finally {
            captureMovementWaterState = false;
            stopMovementInput(client);
        }
        if (!movementWaterStateObserved) {
            throw new AssertionError("The Air rules movement water-state probe did not run");
        }
        if (movementTouchingWater
                || movementSubmergedInWater
                || movementInLava
                || movementSwimming
                || movementWaterHeight != 0.0
                || movementLavaHeight != 0.0) {
            throw new AssertionError(
                    "Air rules exposed a fluid movement state inside local movement: touchingWater="
                            + movementTouchingWater
                            + ", submergedInWater="
                            + movementSubmergedInWater
                            + ", isInLava="
                            + movementInLava
                            + ", swimming="
                            + movementSwimming
                            + ", waterHeight="
                            + movementWaterHeight
                            + ", lavaHeight="
                            + movementLavaHeight);
        }
        assertRealLavaStateOutsideMovement(player);
    }

    private static void resetMovementStateCapture() {
        movementWaterStateObserved = false;
        movementTouchingWater = false;
        movementSubmergedInWater = false;
        movementInLava = false;
        movementSwimming = false;
        movementWaterHeight = 0.0;
        movementLavaHeight = 0.0;
    }

    private static void stopMovementInput(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
    }

    private static void assertVelocity(String behavior, double expected, double actual) {
        assertClose("Lava used the wrong " + behavior, expected, actual, TOLERANCE);
    }

    private static void assertClose(String message, double expected, double actual, double tolerance) {
        if (Math.abs(actual - expected) <= tolerance) {
            return;
        }
        throw new AssertionError(message + ": expected " + expected + " but got " + actual);
    }

    private enum State {
        OPEN_WORLD_CREATION,
        CREATE_WORLD,
        RUN_TEST,
        COMPLETE
    }
}
