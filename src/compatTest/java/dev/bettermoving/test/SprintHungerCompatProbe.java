package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.compat.ClientPlayerSprintProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class SprintHungerCompatProbe {
    private static final int MOVEMENT_TICKS = 32;
    private static final double TOLERANCE = 1.0E-6;

    private SprintHungerCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        Vec3d originalPosition = player.getPos();
        int originalFood = player.getHungerManager().getFoodLevel();
        float originalSaturation = player.getHungerManager().getSaturationLevel();
        boolean allowedFlying = player.getAbilities().allowFlying;
        BlockPos floor = player.getBlockPos().withY(256);
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        player.getAbilities().allowFlying = false;
        try {
            verifyConfiguration();
            prepareFloor(client, floor);
            verifyFoodBoundary(client, floor);
            List<Sample> fullFood = measure(client, floor, 20, false, false);
            for (int food : new int[] {0, 1, 6, 7, 20}) {
                List<Sample> overridden = measure(client, floor, food, true, false);
                compareMovement("Food level " + food, fullFood, overridden);
            }
            List<Sample> hungry = measure(client, floor, 6, false, false);
            check(hungry.stream().noneMatch(Sample::sprinting), "Disabled option allowed low-food sprinting");
            check(hungry.get(MOVEMENT_TICKS - 1).position.x < fullFood.get(MOVEMENT_TICKS - 1).position.x,
                    "Disabled low-food movement did not remain ordinary walking");
            compareMovement("Virtual platform with food level 0",
                    measure(client, floor, 20, false, true),
                    measure(client, floor, 0, true, true));
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
            verifyMaintainingAndDisabling(client, floor);
            verifyOtherRequirements(client, floor);
            BetterMovingClient.LOGGER.info("Sprint hunger checks passed: food 0..20, vanilla speed, toggles, and restrictions");
        } finally {
            BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(false);
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
            releaseKeys(client);
            player.getAbilities().allowFlying = allowedFlying;
            player.getHungerManager().setFoodLevel(originalFood);
            player.getHungerManager().setSaturationLevel(originalSaturation);
            player.setPosition(originalPosition);
            player.setVelocity(Vec3d.ZERO);
            player.setSprinting(false);
            player.setSneaking(false);
            player.setSwimming(false);
            player.setPose(EntityPose.STANDING);
            player.setOnGround(false);
            player.baseTick();
        }
    }

    private static void verifyConfiguration() {
        var option = BetterMovingConfigs.IGNORE_SPRINT_HUNGER;
        check(!option.getDefaultBooleanValue(), "Sprint hunger override must default to off");
        check(option.getKeybind().getDefaultStringValue().isEmpty(), "Toggle hotkey must default to unbound");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(option), "Missing MaLiLib option");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(option), "Missing registered hotkey");
        option.setBooleanValue(false);
        KeybindMulti keybind = (KeybindMulti) option.getKeybind();
        check(keybind.getCallback().onKeyAction(KeyAction.PRESS, keybind), "Toggle callback was not handled");
        check(option.getBooleanValue(), "Hotkey did not enable the option");
        keybind.setValueFromString("LEFT_ALT,H");
        JsonObject serialized = new JsonObject();
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        option.setBooleanValue(false);
        keybind.setValueFromString("");
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(option.getBooleanValue() && "LEFT_ALT,H".equals(keybind.getStringValue()),
                "Toggle state or hotkey did not persist");
        keybind.getCallback().onKeyAction(KeyAction.PRESS, keybind);
        check(!option.getBooleanValue(), "Hotkey did not disable the option");
        keybind.setValueFromString("");
    }

    private static void verifyFoodBoundary(MinecraftClient client, BlockPos floor) {
        resetPlayer(client, floor);
        ClientPlayerEntity player = client.player;
        ClientPlayerSprintProbe probe = (ClientPlayerSprintProbe) player;
        float saturation = player.getHungerManager().getSaturationLevel();
        double speed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        Vec3d velocity = player.getVelocity();
        for (int food = 0; food <= 20; ++food) {
            player.getHungerManager().setFoodLevel(food);
            BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(false);
            check(probe.bettermovingTest$canSprint() == (food > 6), "Vanilla food boundary changed at " + food);
            BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(true);
            check(probe.bettermovingTest$canSprint(), "Enabled food check rejected food level " + food);
            check(player.getHungerManager().getFoodLevel() == food, "Sprint query changed real food level");
            close("Sprint query saturation", saturation, player.getHungerManager().getSaturationLevel());
            close("Sprint query movement attribute", speed, player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
            check(player.getVelocity().equals(velocity), "Sprint query directly changed velocity");
        }
        player.getHungerManager().setFoodLevel(0);
        client.player = null;
        try {
            check(!probe.bettermovingTest$canSprint(), "Override applied to a player that is not the active local player");
        } finally {
            client.player = player;
        }
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(false);
        player.getAbilities().allowFlying = true;
        check(probe.bettermovingTest$canSprint(), "Vanilla creative eligibility was changed");
        player.getAbilities().allowFlying = false;
    }

    private static List<Sample> measure(
            MinecraftClient client, BlockPos floor, int food, boolean override, boolean platform) {
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
        resetPlayer(client, floor);
        ClientPlayerEntity player = client.player;
        player.getHungerManager().setFoodLevel(food);
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(override);
        BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(platform);
        Vec3d start = player.getPos();
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        List<Sample> result = new ArrayList<>();
        for (int tick = 0; tick < MOVEMENT_TICKS; ++tick) {
            movementTick(player);
            check(player.getHungerManager().getFoodLevel() == food, "Movement override changed real hunger");
            result.add(new Sample(player.getPos().subtract(start), player.getVelocity(), player.isSprinting(),
                    player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
        }
        releaseKeys(client);
        if (override || food > 6) {
            check(result.stream().allMatch(Sample::sprinting), "Normal sprint input did not start and maintain sprinting");
        }
        return result;
    }

    private static void verifyMaintainingAndDisabling(MinecraftClient client, BlockPos floor) {
        resetPlayer(client, floor);
        ClientPlayerEntity player = client.player;
        player.getHungerManager().setFoodLevel(7);
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(false);
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        movementTick(player);
        check(player.isSprinting(), "Food level 7 did not allow vanilla sprinting");
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(true);
        player.getHungerManager().setFoodLevel(6);
        movementTick(player);
        check(player.isSprinting(), "Crossing down to food level 6 stopped enabled sprinting");
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(false);
        movementTick(player);
        check(!player.isSprinting(), "Disabling did not restore the hunger restriction");
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(true);
        movementTick(player);
        check(player.isSprinting(), "Re-enabling did not restore low-food sprint eligibility");
        client.options.forwardKey.setPressed(false);
        movementTick(player);
        check(!player.isSprinting(), "No-forward-input sprint stop was bypassed");
    }

    private static void verifyOtherRequirements(MinecraftClient client, BlockPos floor) {
        resetPlayer(client, floor);
        ClientPlayerEntity player = client.player;
        player.getHungerManager().setFoodLevel(0);
        BetterMovingConfigs.IGNORE_SPRINT_HUNGER.setBooleanValue(true);
        ClientPlayerSprintProbe probe = (ClientPlayerSprintProbe) player;
        player.input.movementForward = 1.0F;
        check(probe.bettermovingTest$canStartSprinting(), "Unrestricted hungry player cannot start sprinting");
        player.input.movementForward = 0.0F;
        check(!probe.bettermovingTest$canStartSprinting(), "Forward-input requirement was bypassed");
        player.input.movementForward = 1.0F;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100));
        try {
            check(!probe.bettermovingTest$canStartSprinting(), "Blindness requirement was bypassed");
        } finally {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        ItemStack originalStack = player.getStackInHand(Hand.MAIN_HAND);
        try {
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.SHIELD));
            player.setCurrentHand(Hand.MAIN_HAND);
            check(player.isUsingItem(), "Item-use fixture did not activate");
            check(!probe.bettermovingTest$canStartSprinting(), "Item-use requirement was bypassed");
        } finally {
            player.clearActiveItem();
            player.setStackInHand(Hand.MAIN_HAND, originalStack);
        }
        resetPlayer(client, floor);
        client.options.sneakKey.setPressed(true);
        // Vanilla derives the crouching slowdown from the previous tick's input.
        movementTick(player);
        movementTick(player);
        check(player.isSneaking() && player.isInSneakingPose(), "Sneaking fixture did not settle");
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        for (int tick = 0; tick < 8; ++tick) {
            movementTick(player);
            check(!player.isSprinting(), "Sneaking sprint-start restriction was bypassed");
        }
        resetPlayer(client, floor);
        for (int y = 0; y < 3; ++y) {
            client.world.setBlockState(floor.east().up(y), Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        try {
            client.options.forwardKey.setPressed(true);
            client.options.sprintKey.setPressed(true);
            for (int tick = 0; tick < 8; ++tick) {
                movementTick(player);
            }
            check(player.horizontalCollision && !player.isSprinting(), "Wall-collision sprint stop was bypassed");
        } finally {
            for (int y = 0; y < 3; ++y) {
                client.world.setBlockState(floor.east().up(y), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private static void prepareFloor(MinecraftClient client, BlockPos floor) {
        for (BlockPos pos : BlockPos.iterate(floor.add(-4, -1, -4), floor.add(16, 3, 4))) {
            client.world.setBlockState(pos,
                    (pos.getY() < floor.getY() ? Blocks.STONE : Blocks.AIR).getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void resetPlayer(MinecraftClient client, BlockPos floor) {
        releaseKeys(client);
        ClientPlayerEntity player = client.player;
        player.setPosition(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        player.setVelocity(Vec3d.ZERO);
        player.setYaw(-90.0F);
        player.setPitch(0.0F);
        player.setNoGravity(false);
        player.getAbilities().flying = false;
        player.setSprinting(false);
        player.setSneaking(false);
        player.setSwimming(false);
        player.setPose(EntityPose.STANDING);
        player.fallDistance = 0.0F;
        player.move(MovementType.SELF, new Vec3d(0.0, -0.1, 0.0));
        movementTick(player);
        movementTick(player);
    }

    private static void movementTick(ClientPlayerEntity player) {
        player.baseTick();
        player.tickMovement();
    }

    private static void releaseKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
    }

    private static void compareMovement(String label, List<Sample> expected, List<Sample> actual) {
        for (int tick = 0; tick < MOVEMENT_TICKS; ++tick) {
            Sample baseline = expected.get(tick);
            Sample sample = actual.get(tick);
            close(label + " position tick " + tick, 0.0, baseline.position.distanceTo(sample.position));
            close(label + " velocity tick " + tick, 0.0, baseline.velocity.distanceTo(sample.velocity));
            close(label + " attribute tick " + tick, baseline.speed, sample.speed);
            check(baseline.sprinting == sample.sprinting, label + " sprint state mismatch at tick " + tick);
        }
    }

    private static void close(String message, double expected, double actual) {
        check(Math.abs(expected - actual) <= TOLERANCE, message + ": expected " + expected + " but got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Sample(Vec3d position, Vec3d velocity, boolean sprinting, double speed) {
    }
}
