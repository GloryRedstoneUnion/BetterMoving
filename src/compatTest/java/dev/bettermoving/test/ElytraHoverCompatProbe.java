package dev.bettermoving.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.bettermoving.compat.RiptideStateProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import dev.bettermoving.entity.ClientFireworkRocketManager;
import dev.bettermoving.physics.ClientRiptide;
import dev.bettermoving.physics.ElytraHover;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class ElytraHoverCompatProbe {
    private static final Vec3d MOMENTUM = new Vec3d(1.0, -0.75, 2.0);
    private static final List<FireworkRocketEntity> ROCKETS = new ArrayList<>();
    private static boolean capturePackets;
    private static int usePackets;

    private ElytraHoverCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        JsonObject saved = new JsonObject();
        ConfigUtils.writeConfigBase(saved, "options", BetterMovingConfigs.OPTIONS);
        Vec3d position = player.getPos();
        Vec3d velocity = player.getVelocity();
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        boolean flying = player.getAbilities().flying;
        boolean ground = player.isOnGround();
        boolean gliding = player.isFallFlying();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        GameMode mode = client.interactionManager.getCurrentGameMode();
        try {
            verifyConfiguration();
            BetterMovingConfigs.VIRTUAL_PLATFORM.setBooleanValue(false);
            BetterMovingConfigs.VOID_PROTECTION_PLATFORM.setBooleanValue(false);
            BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
            BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(false);
            BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.setBooleanValue(false);
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);
            client.interactionManager.setGameMode(GameMode.SURVIVAL);
            player.getAbilities().flying = false;
            player.setPosition(position.x, 240.0, position.z);
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            player.setOnGround(false);
            ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
            player.startFallFlying();
            verifySustainedHover(player);
            verifyVanillaRockets(client);
            verifyInfiniteRockets(client);
            verifyTargetSpeedAndGlideStop(client);
            verifyRiptide(client);
            verifyIsolation(client);
            System.out.println("BetterMoving Elytra hover compatibility probe passed: sustained XYZ stop, propulsion restart/expiry, overlapping and infinite rockets, custom lifetimes, Riptide charges/spin, toggle and player isolation");
        } finally {
            capturePackets = false;
            BetterMovingConfigs.HOVER_WHEN_ELYTRA_UNPOWERED.setBooleanValue(false);
            clearRockets();
            ClientRiptide.clearCharge(player);
            player.clearActiveItem();
            ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
            player.stopFallFlying();
            player.equipStack(EquipmentSlot.CHEST, chest);
            player.setStackInHand(Hand.MAIN_HAND, main);
            player.setStackInHand(Hand.OFF_HAND, off);
            player.setPosition(position);
            player.setVelocity(velocity);
            player.setYaw(yaw);
            player.setPitch(pitch);
            player.getAbilities().flying = flying;
            player.setOnGround(ground);
            client.interactionManager.setGameMode(mode);
            ConfigUtils.readConfigBase(saved, "options", BetterMovingConfigs.OPTIONS);
            if (gliding) {
                player.startFallFlying();
            }
            ClientRiptide.tick(client);
        }
    }

    private static void verifyConfiguration() {
        var option = BetterMovingConfigs.HOVER_WHEN_ELYTRA_UNPOWERED;
        check(!option.getDefaultBooleanValue(), "Hover must default off");
        check(option.getKeybind().getStringValue().isEmpty(), "Hover must default unbound");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(option)
                        && BetterMovingConfigs.ALL_HOTKEYS.contains(option),
                "Hover must appear in the GUI and hotkey registry");
        option.setBooleanValue(false);
        check(((KeybindMulti) option.getKeybind()).getCallback().onKeyAction(KeyAction.PRESS, option.getKeybind())
                        && option.getBooleanValue(), "MaLiLib hotkey must enable hover");
        String binding = option.getKeybind().getStringValue();
        option.getKeybind().setValueFromString("F9");
        JsonObject json = new JsonObject();
        ConfigUtils.writeConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        option.setBooleanValue(false);
        option.getKeybind().setValueFromString("");
        ConfigUtils.readConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        check(option.getBooleanValue() && option.getKeybind().getStringValue().equals("F9"),
                "Hover state and keybind must survive serialization");
        option.getKeybind().setValueFromString(binding);
    }

    private static void verifySustainedHover(ClientPlayerEntity player) {
        Vec3d position = player.getPos();
        for (float pitch : new float[] {-90.0F, -30.0F, 0.0F, 30.0F, 90.0F}) {
            player.setPitch(pitch);
            for (int tick = 0; tick < 20; tick++) {
                player.setVelocity(MOMENTUM);
                player.travel(new Vec3d(1.0, 1.0, 1.0));
                check(player.getPos().equals(position), "Unpowered gliding must not drift at pitch " + pitch);
                assertStopped(player, "Unpowered glide");
                check(player.isFallFlying(), "Hover must retain the Elytra glide");
            }
        }
        for (int tick = 0; tick < 20; tick++) {
            player.tickMovement();
            check(player.getPos().equals(position), "Natural unpowered ticks must keep the same position");
            assertStopped(player, "Natural hovering tick");
        }
        player.setPitch(0.0F);
    }

    private static void verifyVanillaRockets(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(false);
        FireworkRocketEntity first = rocket(client, false, 1);
        FireworkRocketEntity second = rocket(client, false, 3);
        check(!ElytraHover.shouldHover(player), "Attached vanilla rocket must permit travel before its first tick");
        first.setId(-123456);
        check(ClientFireworkRocketManager.hasActiveBoost(player), "Changing entity ID must preserve tracking");
        first.tick();
        Vec3d position = player.getPos();
        player.travel(Vec3d.ZERO);
        check(player.getPos().distanceTo(position) > 0.0, "Vanilla firework propulsion must move the player");
        first.discard();
        check(!ElytraHover.shouldHover(player) && player.getVelocity().length() > 0.0,
                "Removing one rocket must preserve another active rocket");
        for (int tick = 0; tick < 100; tick++) {
            second.tick();
        }
        check(!ElytraHover.shouldHover(player),
                "Server-owned rockets must stay powered until server removal, regardless of client lifetime");
        second.discard();
        assertStopped(player, "Last vanilla rocket removal");
        verifySustainedHover(player);
        clearRockets();

        // Mirror the network spawn, metadata, and removal path before its first tick.
        FireworkRocketEntity template = rocket(client, false, 1);
        FireworkRocketEntity networkRocket = new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, client.world);
        networkRocket.getDataTracker().writeUpdatedEntries(template.getDataTracker().getChangedEntries());
        networkRocket.setId(-123455);
        client.world.addEntity(networkRocket.getId(), networkRocket);
        ROCKETS.add(networkRocket);
        template.discard();
        check(!ElytraHover.shouldHover(player), "Incoming shooter metadata must enable propulsion before first tick");
        networkRocket.tick();
        check(player.getVelocity().length() > 0.0, "Network-spawned rocket must accelerate normally");
        client.world.removeEntity(networkRocket.getId(), Entity.RemovalReason.DISCARDED);
        assertStopped(player, "Network removal of last attached rocket");
        clearRockets();

        // A free firework or one attached to someone else is not local propulsion.
        OtherClientPlayerEntity remote = remotePlayer(client);
        FireworkRocketEntity other = new FireworkRocketEntity(client.world, firework(1), remote);
        FireworkRocketEntity free = new FireworkRocketEntity(client.world,
                player.getX(), player.getY(), player.getZ(), firework(1));
        ROCKETS.add(other);
        ROCKETS.add(free);
        player.setVelocity(MOMENTUM);
        player.travel(Vec3d.ZERO);
        assertStopped(player, "Other and free rockets");
        clearRockets();
    }

    private static void verifyInfiniteRockets(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        for (boolean custom : new boolean[] {false, true}) {
            BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(custom);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(2);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.setIntegerValue(2);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.setIntegerValue(2);
            for (int flight : new int[] {1, 2, 3}) {
                FireworkRocketEntity rocket = rocket(client, true, flight);
                for (int tick = 0; tick < 100 && !rocket.isRemoved(); tick++) {
                    check(!ElytraHover.shouldHover(player), "Live simulated rocket must permit motion");
                    rocket.tick();
                    if (!rocket.isRemoved()) {
                        Vec3d position = player.getPos();
                        player.travel(Vec3d.ZERO);
                        check(player.getPos().distanceTo(position) > 0.0,
                                "Infinite firework must resume movement from hover");
                    }
                }
                check(rocket.isRemoved(), "Simulated rocket must expire");
                assertStopped(player, "Infinite firework expiry: custom=" + custom + ", Flight=" + flight);
                Vec3d position = player.getPos();
                player.travel(Vec3d.ZERO);
                check(player.getPos().equals(position), "Expired rocket must leave no inertial drift");
                clearRockets();
            }
        }

        // Check the production item-use route still suppresses packets and consumption.
        ItemStack stack = firework(1);
        stack.setCount(3);
        player.setStackInHand(Hand.MAIN_HAND, stack);
        capturePackets = true;
        usePackets = 0;
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND).isAccepted(),
                "Infinite firework use must work while hovering");
        check(stack.getCount() == 3 && usePackets == 0, "Infinite firework use must stay local");
        for (var entity : client.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity rocket && rocket.getOwner() == player) {
                ROCKETS.add(rocket);
            }
        }
        check(ClientFireworkRocketManager.hasActiveBoost(player), "Infinite use must create active propulsion");
        clearRockets();
        capturePackets = false;

        // Expiration of one source must not stop another propulsion source.
        FireworkRocketEntity remaining = rocket(client, true, 1);
        FireworkRocketEntity expiring = rocket(client, true, 1);
        for (int tick = 0; tick < 3; tick++) {
            expiring.tick();
        }
        check(expiring.isRemoved() && !ElytraHover.shouldHover(player),
                "Overlapping simulated rockets must remain powered until all expire");
        remaining.discard();
        assertStopped(player, "Final overlapping rocket expiry");
        clearRockets();
    }

    private static void verifyTargetSpeedAndGlideStop(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.setDoubleValue(40.0);
        BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(100);
        FireworkRocketEntity first = rocket(client, true, 1);
        player.setVelocity(player.getRotationVector().multiply(2.0));
        first.tick();
        check(Math.abs(player.getVelocity().length() - 2.0) < 1.0E-6
                        && !ElytraHover.shouldHover(player),
                "Firework equilibrium with no speed increase must still count as propulsion");
        FireworkRocketEntity second = rocket(client, true, 1);
        BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.setBooleanValue(true);
        player.stopFallFlying();
        check(first.isRemoved() && second.isRemoved(),
                "Glide stopping must cancel all simulated rockets while hover is enabled");
        player.startFallFlying();
        player.setVelocity(MOMENTUM);
        player.travel(Vec3d.ZERO);
        assertStopped(player, "Glide restart after firework cancellation");
        clearRockets();
        BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.setBooleanValue(false);
        BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.setBooleanValue(false);
    }

    private static void verifyRiptide(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);
        for (boolean custom : new boolean[] {false, true}) {
            BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(custom);
            int[] requirements = custom ? new int[] {0, 5, 10, 20, Integer.MAX_VALUE} : new int[] {10};
            for (Hand hand : Hand.values()) {
                for (int required : requirements) {
                    verifyRiptideCharge(client, hand, required);
                }
            }
        }
        // A stale server spin flag must not count as an active impulse.
        player.getDataTracker().set(RiptideStateProbe.bettermovingTest$getLivingFlags(), (byte) 4);
        check(ElytraHover.shouldHover(player), "Stale Riptide metadata must not bypass hover");
        player.getDataTracker().set(RiptideStateProbe.bettermovingTest$getLivingFlags(), (byte) 0);
        player.useRiptide(20);
        player.setVelocity(MOMENTUM);
        player.horizontalCollision = true;
        ((RiptideStateProbe) player).bettermovingTest$tickRiptide(
                player.getBoundingBox(), player.getBoundingBox());
        assertStopped(player, "Riptide collision ends propulsion");
        player.horizontalCollision = false;
    }

    private static void verifyRiptideCharge(MinecraftClient client, Hand hand, int required) {
        ClientPlayerEntity player = client.player;
        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.addEnchantment(Enchantments.RIPTIDE, 3);
        player.setStackInHand(hand, trident);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(required);
        capturePackets = true;
        usePackets = 0;
        ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
        check(client.interactionManager.interactItem(player, hand).isAccepted(),
                "Simulated Riptide must charge while hovering");
        check(ElytraHover.shouldHover(player), "Charging must not count as propulsion");
        player.setVelocity(MOMENTUM);
        Vec3d before = player.getPos();
        player.travel(Vec3d.ZERO);
        assertStopped(player, "Hover during Riptide charge");
        check(player.getPos().equals(before), "Riptide charging must not allow drift");
        if (required > 0) {
            ((RiptideStateProbe) player).bettermovingTest$setItemUseTimeLeft(
                    trident.getMaxUseTime() - (required - 1));
            client.interactionManager.stopUsingItem(player);
            check(ElytraHover.shouldHover(player), "Short charge must stay unpowered");
            check(client.interactionManager.interactItem(player, hand).isAccepted(),
                    "Riptide must allow charging again");
        }
        ((RiptideStateProbe) player).bettermovingTest$setItemUseTimeLeft(
                trident.getMaxUseTime() - required);
        client.interactionManager.stopUsingItem(player);
        check(!ElytraHover.shouldHover(player) && player.getVelocity().length() > 0.0,
                "Successful Riptide release must resume movement");
        FireworkRocketEntity overlapping = rocket(client, true, 1);
        overlapping.discard();
        check(!ElytraHover.shouldHover(player), "Rocket removal must not stop an active Riptide");
        clearRockets();
        for (int tick = 0; tick < 20; tick++) {
            player.tickMovement();
            check(ElytraHover.shouldHover(player) == (tick == 19),
                    "Only the last Riptide tick must enter hover");
        }
        assertStopped(player, "Riptide spin expiry");
        before = player.getPos();
        player.tickMovement();
        check(player.getPos().equals(before), "Expired Riptide must not leave drift");
        check(player.isFallFlying(), "Riptide expiry must preserve Elytra flight");
        check(trident.getDamage() == 0 && usePackets == 0,
                "Hover must preserve local Riptide packet and durability rules");
        capturePackets = false;
    }

    private static void verifyIsolation(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.HOVER_WHEN_ELYTRA_UNPOWERED.setBooleanValue(false);
        player.setVelocity(MOMENTUM);
        Vec3d position = player.getPos();
        player.travel(Vec3d.ZERO);
        check(player.getPos().distanceTo(position) > 0.0, "Disabling hover must restore ordinary gliding");
        BetterMovingConfigs.HOVER_WHEN_ELYTRA_UNPOWERED.setBooleanValue(true);
        player.stopFallFlying();
        player.setVelocity(Vec3d.ZERO);
        player.travel(Vec3d.ZERO);
        check(player.getVelocity().y < 0.0, "Non-gliding players must retain gravity");
        player.setOnGround(true);
        player.startFallFlying();
        check(!ElytraHover.shouldHover(player), "Ground movement must remain vanilla");
        player.setOnGround(false);
        player.getAbilities().flying = true;
        check(!ElytraHover.shouldHover(player), "Creative flight must remain vanilla");
        player.getAbilities().flying = false;
        OtherClientPlayerEntity remote = remotePlayer(client);
        remote.setVelocity(MOMENTUM);
        check(!ElytraHover.shouldHover(remote), "Remote players must not hover");
        ElytraHover.stopIfUnpowered(remote);
        check(remote.getVelocity().equals(MOMENTUM), "Remote momentum must be unchanged");
        client.player = null;
        check(!ElytraHover.shouldHover(player), "Disconnected or replaced player must not hover");
        client.player = player;
    }

    private static FireworkRocketEntity rocket(MinecraftClient client, boolean simulated, int flight) {
        FireworkRocketEntity rocket = new FireworkRocketEntity(client.world, firework(flight), client.player);
        if (simulated) {
            ((ClientFireworkRocket) rocket).bettermoving$markLocalSimulation();
        }
        ROCKETS.add(rocket);
        return rocket;
    }

    private static ItemStack firework(int flight) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.getOrCreateSubNbt("Fireworks").putByte("Flight", (byte) flight);
        return stack;
    }

    private static OtherClientPlayerEntity remotePlayer(MinecraftClient client) {
        OtherClientPlayerEntity remote = new OtherClientPlayerEntity(client.world,
                new GameProfile(UUID.randomUUID(), "HoverProbe"));
        remote.setPosition(client.player.getPos());
        remote.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        remote.startFallFlying();
        return remote;
    }

    private static void clearRockets() {
        for (FireworkRocketEntity rocket : ROCKETS) {
            rocket.discard();
        }
        ROCKETS.clear();
    }

    public static void observePacket(Packet<?> packet) {
        if (capturePackets && (packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerActionC2SPacket action
                        && action.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM)) {
            usePackets++;
        }
    }

    private static void assertStopped(ClientPlayerEntity player, String stage) {
        check(player.getVelocity().equals(Vec3d.ZERO), stage + " must immediately clear all axes: " + player.getVelocity());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
