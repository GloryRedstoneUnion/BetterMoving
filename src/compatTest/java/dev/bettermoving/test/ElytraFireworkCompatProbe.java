package dev.bettermoving.test;

import dev.bettermoving.compat.ClientWorldPredictionProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import fi.dy.masa.malilib.config.ConfigUtils;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class ElytraFireworkCompatProbe {
    private static boolean capturePackets;
    private static int blockUsePackets;
    private static int itemUsePackets;
    private static int entityUsePackets;

    private ElytraFireworkCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ItemStack previousChest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack previousMainHand = player.getMainHandStack();
        ItemStack previousOffHand = player.getOffHandStack();
        boolean previousSneaking = player.input.sneaking;
        GameMode previousGameMode = client.interactionManager.getCurrentGameMode();
        boolean previousToggle = BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue();
        boolean previousBlockUseToggle = BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.getBooleanValue();
        boolean previousBlockNonElytraUseToggle =
                BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.getBooleanValue();
        boolean previousTargetSpeedToggle =
                BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.getBooleanValue();
        boolean previousLifetimeToggle =
                BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.getBooleanValue();
        int previousFlight1Lifetime = BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getIntegerValue();
        int previousFlight2Lifetime = BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.getIntegerValue();
        int previousFlight3Lifetime = BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.getIntegerValue();
        double previousTargetSpeed =
                BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.getDoubleValue();
        try {
            verifyConfiguration();
            capturePackets = true;
            player.input.sneaking = false;
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
            player.setOnGround(false);
            player.setVelocity(Vec3d.ZERO);
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);
            BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.setBooleanValue(true);
            BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(true);
            BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.setDoubleValue(40.0);

            BlockPos target = player.getBlockPos().down();
            client.world.setBlockState(target, Blocks.STONE.getDefaultState());
            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(false);
            player.stopFallFlying();
            ActionResult result = interactBlock(client, Hand.MAIN_HAND, target);
            if (result != ActionResult.FAIL || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Non-Elytra firework block use was not fully blocked on the ground: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }
            assertPackets("blocked ground launch", 0, 0, 0);

            verifyNonLaunchInteractions(client, target);
            client.interactionManager.setGameMode(previousGameMode);
            client.world.setBlockState(target, Blocks.STONE.getDefaultState());

            player.startFallFlying();
            result = interactBlock(client, Hand.MAIN_HAND, target);
            if (result != ActionResult.FAIL || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Firework block use was not blocked while gliding without the Elytra route: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }
            assertPackets("blocked gliding launch", 0, 0, 0);

            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(true);
            result = interactBlock(client, Hand.MAIN_HAND, target);
            if (!result.isAccepted() || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Elytra firework block use did not preserve the local rocket stack: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }
            assertPackets("infinite Elytra boost", 0, 0, 0);

            FireworkRocketEntity rocket = client.world.getEntitiesByType(
                            TypeFilter.instanceOf(FireworkRocketEntity.class),
                            player.getBoundingBox().expand(2.0),
                            entity -> true)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No local Elytra firework entity was created"));
            rocket.tick();
            if (Math.abs(player.getVelocity().x) > 1.0E-6
                    || Math.abs(player.getVelocity().y) > 1.0E-6
                    || Math.abs(player.getVelocity().z - 1.0) > 1.0E-6) {
                throw new AssertionError(
                        "The local Elytra firework did not apply the configured target speed: velocity="
                                + player.getVelocity());
            }
            for (int i = 0; i < 200 && !rocket.isRemoved(); ++i) {
                rocket.tick();
            }
            if (!rocket.isRemoved()) {
                throw new AssertionError("The local Elytra firework did not expire after its normal lifetime");
            }

            BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(true);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(3);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.setIntegerValue(6);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.setIntegerValue(9);
            verifyRocketLifetime(client, 1, 3);
            verifyRocketLifetime(client, 2, 6);
            verifyRocketLifetime(client, 3, 9);
            verifyRocketLifetime(client, 4, 20);
            BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(false);
            verifyRocketLifetime(client, 2, 20);

            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(false);
            result = interactBlock(client, Hand.MAIN_HAND, target);
            if (!result.isAccepted()) {
                throw new AssertionError("The normal Elytra firework boost was blocked: " + result);
            }
            assertPackets("normal Elytra boost", 0, 1, 0);

            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(false);
            BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.setBooleanValue(false);
            player.stopFallFlying();
            result = interactBlock(client, Hand.MAIN_HAND, target);
            if (!result.isAccepted()) {
                throw new AssertionError("Disabling firework suppression did not restore vanilla use: " + result);
            }
            assertPackets("disabled suppression", 1, 0, 0);
            System.out.println("[bettermoving] Elytra firework compatibility checks passed");
        } finally {
            capturePackets = false;
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(previousToggle);
            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(previousBlockUseToggle);
            BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.setBooleanValue(previousBlockNonElytraUseToggle);
            BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(previousTargetSpeedToggle);
            BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.setDoubleValue(previousTargetSpeed);
            BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(previousLifetimeToggle);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(previousFlight1Lifetime);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.setIntegerValue(previousFlight2Lifetime);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.setIntegerValue(previousFlight3Lifetime);
            player.stopFallFlying();
            player.equipStack(EquipmentSlot.CHEST, previousChest);
            player.equipStack(EquipmentSlot.MAINHAND, previousMainHand);
            player.equipStack(EquipmentSlot.OFFHAND, previousOffHand);
            player.input.sneaking = previousSneaking;
            client.interactionManager.setGameMode(previousGameMode);
            player.setVelocity(Vec3d.ZERO);
        }
    }

    private static void verifyConfiguration() {
        check(!BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.getDefaultBooleanValue(),
                "Simulated Elytra firework target speed must default to disabled");
        check(!BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.getDefaultBooleanValue(),
                "Custom Elytra firework lifetime must default to disabled");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME),
                "Missing custom Elytra firework lifetime GUI toggle");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME),
                "Custom Elytra firework lifetime must be registered as a hotkey");
        check(BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getDefaultIntegerValue() == 0
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.getDefaultIntegerValue() == 0
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.getDefaultIntegerValue() == 0,
                "Custom Elytra firework lifetime values must default to zero ticks");
        check(BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getMaxIntegerValue() == 31
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.getMaxIntegerValue() == 41
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.getMaxIntegerValue() == 51,
                "Custom Elytra firework lifetime limits must match vanilla Flight 1/2/3 maxima");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1)
                        && BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2)
                        && BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3),
                "All custom Elytra firework lifetime values must be in the GUI");
        check(Math.abs(BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED
                .getDefaultDoubleValue() - 64.0) < 1.0E-9,
                "Simulated Elytra firework target speed must default to 64 m/s");
        check(BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.getMaxDoubleValue()
                        == Double.MAX_VALUE,
                "Simulated Elytra firework target speed must accept the full double range");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED),
                "Missing simulated Elytra firework speed GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(
                        BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED),
                "Missing simulated Elytra firework target speed value GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.indexOf(
                        BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME)
                        == BetterMovingConfigs.GUI_OPTIONS.indexOf(
                                BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED) + 1,
                "Custom Elytra firework lifetime toggle must appear immediately below its target speed toggle");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED),
                "Simulated Elytra firework target speed must be registered as a hotkey");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(
                        BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED),
                "Simulated Elytra firework target speed value must not be registered as a hotkey");

        JsonObject serialized = new JsonObject();
        BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.setDoubleValue(37.5);
        BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(true);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(11);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.setIntegerValue(22);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.setIntegerValue(33);
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(false);
        BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.setDoubleValue(64.0);
        BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(false);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(0);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.setIntegerValue(0);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.setIntegerValue(0);
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.getBooleanValue(),
                "Simulated Elytra firework target speed toggle persistence was not restored");
        check(Math.abs(BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED
                .getDoubleValue() - 37.5) < 1.0E-9,
                "Simulated Elytra firework target speed persistence was not restored");
        check(BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.getBooleanValue()
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getIntegerValue() == 11
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.getIntegerValue() == 22
                        && BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.getIntegerValue() == 33,
                "Custom Elytra firework lifetime settings persistence was not restored");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void verifyRocketLifetime(MinecraftClient client, int flight, int expectedLifetime) {
        ClientPlayerEntity player = client.player;
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        NbtCompound fireworks = new NbtCompound();
        fireworks.putByte("Flight", (byte) flight);
        stack.setSubNbt("Fireworks", fireworks);

        FireworkRocketEntity rocket = new FireworkRocketEntity(client.world, stack, player);
        NbtCompound savedState = new NbtCompound();
        savedState.putInt("Life", 0);
        savedState.putInt("LifeTime", 20);
        rocket.readCustomDataFromNbt(savedState);
        ((ClientFireworkRocket) rocket).bettermoving$markLocalSimulation();

        for (int i = 0; i < expectedLifetime; ++i) {
            rocket.tick();
            if (rocket.isRemoved()) {
                throw new AssertionError("Flight " + flight + " firework expired before tick "
                        + (i + 1) + " of expected lifetime " + expectedLifetime);
            }
        }
        rocket.tick();
        if (!rocket.isRemoved()) {
            throw new AssertionError("Flight " + flight + " firework did not expire after "
                    + expectedLifetime + " ticks");
        }
    }

    private static void verifyNonLaunchInteractions(MinecraftClient client, BlockPos target) {
        ClientPlayerEntity player = client.player;
        player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
        client.world.setBlockState(target.up(), Blocks.AIR.getDefaultState());
        for (GameMode gameMode : new GameMode[] {GameMode.CREATIVE, GameMode.SURVIVAL}) {
            client.interactionManager.setGameMode(gameMode);
            for (boolean gliding : new boolean[] {false, true}) {
                if (gliding) {
                    player.startFallFlying();
                } else {
                    player.stopFallFlying();
                }
                for (Hand hand : Hand.values()) {
                    client.world.setBlockState(target, Blocks.STONE.getDefaultState());
                    assertBlockedLaunch(client, hand, target);
                    for (Block block : new Block[] {Blocks.CRAFTING_TABLE, Blocks.CHEST, Blocks.BARREL, Blocks.LEVER}) {
                        client.world.setBlockState(target, block.getDefaultState());
                        ActionResult result = interactBlock(client, hand, target);
                        if (!result.isAccepted() || player.getStackInHand(hand).getCount() != 3) {
                            throw new AssertionError("Firework suppression blocked a normal interaction: block="
                                    + block + ", hand=" + hand + ", mode=" + gameMode
                                    + ", gliding=" + gliding + ", result=" + result);
                        }
                        assertPackets("normal " + block + " interaction", 1, 0, 0);
                    }
                    player.input.sneaking = true;
                    try {
                        client.world.setBlockState(target, Blocks.CRAFTING_TABLE.getDefaultState());
                        assertBlockedLaunch(client, hand, target);
                    } finally {
                        player.input.sneaking = false;
                    }
                }
            }
        }

        player.stopFallFlying();
        VillagerEntity villager = new VillagerEntity(EntityType.VILLAGER, client.world);
        villager.setId(-1_000_000);
        for (Hand hand : Hand.values()) {
            resetPacketCounts();
            ActionResult result = client.interactionManager.interactEntity(player, villager, hand);
            if (!result.isAccepted() || player.getStackInHand(hand).getCount() != 3) {
                throw new AssertionError("Firework suppression blocked a villager interaction: " + result);
            }
            assertPackets("villager interaction", 0, 0, 1);
        }

        player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        client.world.setBlockState(target, Blocks.STONE.getDefaultState());
        ActionResult result = interactBlock(client, Hand.MAIN_HAND, target);
        if (result != ActionResult.PASS) {
            throw new AssertionError("An off-hand firework incorrectly blocked the main-hand item: " + result);
        }
        assertPackets("other main-hand item", 1, 0, 0);
        player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
    }

    private static void assertBlockedLaunch(MinecraftClient client, Hand hand, BlockPos target) {
        int rocketsBefore = countRockets(client);
        ActionResult result = interactBlock(client, hand, target);
        if (result != ActionResult.FAIL || result.shouldSwingHand()
                || client.player.getStackInHand(hand).getCount() != 3
                || countRockets(client) != rocketsBefore) {
            throw new AssertionError("A blocked firework launch had a side effect: hand=" + hand + ", result=" + result);
        }
        assertPackets("blocked launch", 0, 0, 0);
    }

    private static int countRockets(MinecraftClient client) {
        return client.world.getEntitiesByType(TypeFilter.instanceOf(FireworkRocketEntity.class),
                client.player.getBoundingBox().expand(8.0), entity -> true).size();
    }

    private static ActionResult interactBlock(MinecraftClient client, Hand hand, BlockPos target) {
        resetPacketCounts();
        ActionResult result = client.interactionManager.interactBlock(client.player, hand,
                new BlockHitResult(Vec3d.ofCenter(target), Direction.NORTH, target, false));
        if (((ClientWorldPredictionProbe) client.world)
                .bettermovingTest$getPendingUpdateManager().hasPendingSequence()) {
            throw new AssertionError("A firework interaction left block prediction active");
        }
        return result;
    }

    public static void observePacket(Packet<?> packet) {
        if (!capturePackets) {
            return;
        }
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            ++blockUsePackets;
        } else if (packet instanceof PlayerInteractItemC2SPacket) {
            ++itemUsePackets;
        } else if (packet instanceof PlayerInteractEntityC2SPacket) {
            ++entityUsePackets;
        }
    }

    private static void resetPacketCounts() {
        blockUsePackets = 0;
        itemUsePackets = 0;
        entityUsePackets = 0;
    }

    private static void assertPackets(String action, int blocks, int items, int entities) {
        if (blockUsePackets != blocks || itemUsePackets != items || entityUsePackets != entities) {
            throw new AssertionError("Unexpected interaction packets for " + action
                    + ": block=" + blockUsePackets + ", item=" + itemUsePackets + ", entity=" + entityUsePackets);
        }
    }
}
