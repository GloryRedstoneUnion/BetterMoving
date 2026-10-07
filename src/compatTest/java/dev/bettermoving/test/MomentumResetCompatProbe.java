package dev.bettermoving.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.bettermoving.compat.EntityFlagsProbe;
import dev.bettermoving.compat.RiptideStateProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import dev.bettermoving.entity.ClientFireworkRocketManager;
import dev.bettermoving.physics.LevitationElytraFlight;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class MomentumResetCompatProbe {
    private static final int TEST_KEY = GLFW.GLFW_KEY_F8;
    private static final Vec3d MOMENTUM = new Vec3d(2.5, -1.75, -3.25);

    private MomentumResetCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        KeybindMulti keybind = (KeybindMulti) BetterMovingConfigs.RESET_MOMENTUM.getKeybind();
        KeybindSettings previousSettings = keybind.getSettings();
        Screen previousScreen = client.currentScreen;
        JsonObject savedHotkeys = new JsonObject();
        ConfigUtils.writeHotkeys(savedHotkeys, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
        Vec3d previousPosition = player.getPos();
        Vec3d previousVelocity = player.getVelocity();
        ItemStack previousChest = player.getEquippedStack(EquipmentSlot.CHEST);
        float previousFallDistance = player.fallDistance;
        boolean previousFlying = player.getAbilities().flying;
        boolean previousOnGround = player.isOnGround();
        boolean previousSprinting = player.isSprinting();
        boolean previousSwimming = player.isSwimming();
        boolean previousGliding = player.isFallFlying();
        EntityPose previousPose = player.getPose();
        try {
            verifyConfiguration(keybind);
            client.setScreen(null);
            player.setPosition(previousPosition.x, 240.0, previousPosition.z);
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            keybind.setValueFromString("F8");
            InputEventHandler.getKeybindManager().updateUsedKeys();

            OtherClientPlayerEntity remote = new OtherClientPlayerEntity(client.world,
                    new GameProfile(UUID.randomUUID(), "MomentumProbe"));
            remote.setVelocity(MOMENTUM);
            remote.startFallFlying();
            for (String state : new String[] {"walking", "sprinting", "falling", "swimming",
                    "creative flight", "Elytra gliding", "Riptide"}) {
                player.stopFallFlying();
                player.setSwimming(false);
                player.setSprinting(false);
                player.getAbilities().flying = false;
                player.setOnGround(false);
                player.setPose(EntityPose.STANDING);
                ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
                switch (state) {
                    case "walking" -> player.setOnGround(true);
                    case "sprinting" -> {
                        player.setOnGround(true);
                        player.setSprinting(true);
                    }
                    case "swimming" -> {
                        player.setSwimming(true);
                        player.setPose(EntityPose.SWIMMING);
                    }
                    case "creative flight" -> player.getAbilities().flying = true;
                    case "Elytra gliding" -> player.startFallFlying();
                    case "Riptide" -> ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(20);
                }
                player.fallDistance = 14.0F;
                boolean swimming = player.isSwimming();
                boolean sprinting = player.isSprinting();
                boolean spinning = player.isUsingRiptide();
                Vec3d position = player.getPos();
                player.setVelocity(MOMENTUM);
                check(input(TEST_KEY, true), state + ": press must consume the hotkey");
                check(player.getVelocity().equals(Vec3d.ZERO), state + ": all three axes must reset");
                check(player.getPos().equals(position), state + ": position must not change");
                check(player.fallDistance == 14.0F, state + ": fall distance must not change");
                check(!player.isFallFlying(), state + ": an active Elytra glide must end");
                check(player.isSwimming() == swimming
                                && player.isSprinting() == sprinting && player.isUsingRiptide() == spinning,
                        state + ": other movement states must not change");
                check(remote.getVelocity().equals(MOMENTUM) && remote.isFallFlying(),
                        state + ": remote players must be unaffected");

                player.setVelocity(MOMENTUM.negate());
                if (state.equals("Elytra gliding")) {
                    player.startFallFlying();
                }
                for (int tick = 0; tick < 3; tick++) {
                    TickHandler.getInstance().onClientTick(client);
                    check(!input(TEST_KEY, true), state + ": held/repeated input must not retrigger");
                    check(player.getVelocity().equals(MOMENTUM.negate()), state + ": holding must preserve new motion");
                    if (state.equals("Elytra gliding")) {
                        check(player.isFallFlying(), "Holding must not stop a newly started glide");
                    }
                }
                check(!input(TEST_KEY, false), state + ": release must not trigger");
                check(player.getVelocity().equals(MOMENTUM.negate()), state + ": release must preserve velocity");
            }

            verifyInputContext(client, keybind);
            verifyMotionResumes(player);
            verifyGlideStops(client);
            client.player = null;
            check(!keybind.getCallback().onKeyAction(KeyAction.PRESS, keybind),
                    "Missing player must be safely ignored");
            System.out.println("BetterMoving momentum reset compatibility probe passed: press edges, states, context, persistence, resumed motion, glide stopping, and firework/Levitation integration");
        } finally {
            client.player = player;
            input(TEST_KEY, false);
            input(GLFW.GLFW_KEY_W, false);
            ConfigUtils.readHotkeys(savedHotkeys, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
            keybind.setSettings(previousSettings);
            InputEventHandler.getKeybindManager().updateUsedKeys();
            client.setScreen(previousScreen);
            player.stopFallFlying();
            ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
            player.equipStack(EquipmentSlot.CHEST, previousChest);
            player.setPosition(previousPosition);
            player.setVelocity(previousVelocity);
            player.fallDistance = previousFallDistance;
            player.getAbilities().flying = previousFlying;
            player.setOnGround(previousOnGround);
            player.setSprinting(previousSprinting);
            player.setSwimming(previousSwimming);
            player.setPose(previousPose);
            if (previousGliding) {
                player.startFallFlying();
            }
        }
    }

    private static void verifyConfiguration(KeybindMulti keybind) {
        var option = BetterMovingConfigs.RESET_MOMENTUM;
        check(option.getDefaultStringValue().isEmpty(), "Action must default unbound");
        keybind.resetSettingsToDefaults();
        KeybindSettings settings = keybind.getSettings();
        check(settings.getActivateOn() == KeyAction.PRESS, "Default action must be PRESS");
        check(settings.getContext() == KeybindSettings.Context.INGAME, "Default context must be INGAME");
        check(!settings.getAllowEmpty() && !settings.getAllowExtraKeys() && settings.isOrderSensitive()
                        && !settings.isExclusive() && settings.shouldCancel(),
                "Advanced defaults must match the requested settings");
        check(BetterMovingConfigs.STANDALONE_HOTKEYS.contains(option)
                        && BetterMovingConfigs.ALL_HOTKEYS.contains(option),
                "Action must be persisted and registered with MaLiLib");
        check(BetterMovingConfigs.GUI_OPTIONS.get(0) == BetterMovingConfigs.OPEN_CONFIG_GUI
                        && BetterMovingConfigs.GUI_OPTIONS.get(1) == option,
                "Action must follow the existing config-screen hotkey");
        check(!BetterMovingConfigs.OPTIONS.contains(option), "Action must not be stored as a toggle");
        keybind.setValueFromString("F8");
        JsonObject json = new JsonObject();
        ConfigUtils.writeHotkeys(json, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
        keybind.setValueFromString("");
        ConfigUtils.readHotkeys(json, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
        check(keybind.getStringValue().equals("F8") && keybind.getSettings().equals(settings),
                "Saved binding must load with the default advanced settings");
        KeybindSettings custom = KeybindSettings.create(KeybindSettings.Context.INGAME,
                KeyAction.PRESS, true, false, false, false, true);
        keybind.setSettings(custom);
        ConfigUtils.writeHotkeys(json, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
        keybind.setValueFromString("");
        keybind.setSettings(KeybindSettings.GUI);
        ConfigUtils.readHotkeys(json, "hotkeys", BetterMovingConfigs.STANDALONE_HOTKEYS);
        check(keybind.getStringValue().equals("F8") && keybind.getSettings().equals(custom),
                "Customized advanced settings must survive serialization");
        keybind.resetSettingsToDefaults();
    }

    private static void verifyInputContext(MinecraftClient client, KeybindMulti keybind) {
        ClientPlayerEntity player = client.player;
        player.startFallFlying();
        player.setVelocity(MOMENTUM);
        input(GLFW.GLFW_KEY_W, true);
        check(!input(TEST_KEY, true), "Default settings must reject extra movement keys");
        check(player.getVelocity().equals(MOMENTUM) && player.isFallFlying(),
                "Rejected shared input must preserve momentum and gliding");
        input(TEST_KEY, false);
        input(GLFW.GLFW_KEY_W, false);
        check(input(TEST_KEY, true), "Reset must still work after shared input is released");
        check(player.getVelocity().equals(Vec3d.ZERO), "An exact binding must reset momentum");
        input(TEST_KEY, false);

        keybind.setSettings(KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
        player.setVelocity(MOMENTUM);
        input(GLFW.GLFW_KEY_W, true);
        check(input(TEST_KEY, true), "User-enabled sharing must allow extra movement keys");
        check(player.getVelocity().equals(Vec3d.ZERO), "Customized shared input must reset momentum");
        input(TEST_KEY, false);
        input(GLFW.GLFW_KEY_W, false);
        keybind.resetSettingsToDefaults();
        client.setScreen(new InventoryScreen(player));
        player.startFallFlying();
        player.setVelocity(MOMENTUM);
        check(!input(TEST_KEY, true), "In-game hotkey must not activate inside a GUI");
        check(player.getVelocity().equals(MOMENTUM) && player.isFallFlying(),
                "GUI input must preserve momentum and gliding");
        input(TEST_KEY, false);
        client.setScreen(null);
        keybind.setValueFromString("");
        InputEventHandler.getKeybindManager().updateUsedKeys();
        check(!input(TEST_KEY, true), "Empty binding must remain inert");
        check(player.getVelocity().equals(MOMENTUM) && player.isFallFlying(),
                "Unbound action must preserve momentum and gliding");
        input(TEST_KEY, false);
        keybind.setValueFromString("F8");
        InputEventHandler.getKeybindManager().updateUsedKeys();
        check(!keybind.getCallback().onKeyAction(KeyAction.RELEASE, keybind), "Release callback must be ignored");
        check(player.getVelocity().equals(MOMENTUM) && player.isFallFlying(),
                "Release callback must preserve motion and gliding");
    }

    private static void verifyMotionResumes(ClientPlayerEntity player) {
        ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
        player.setOnGround(false);
        player.setSwimming(false);
        player.stopFallFlying();
        player.getAbilities().flying = false;
        player.setVelocity(MOMENTUM);
        check(input(TEST_KEY, true), "Gravity check must trigger reset");
        player.travel(Vec3d.ZERO);
        check(player.getVelocity().y < 0.0, "Gravity must resume after the single reset");
        input(TEST_KEY, false);
    }

    private static void verifyGlideStops(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean previousInfinite = BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue();
        boolean previousCancel = BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.getBooleanValue();
        boolean previousLifetime = BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.getBooleanValue();
        int previousFlight1Lifetime = BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getIntegerValue();
        boolean previousIgnore = BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.getBooleanValue();
        StatusEffectInstance previousLevitation = player.getStatusEffect(StatusEffects.LEVITATION);
        if (previousLevitation != null) {
            previousLevitation = new StatusEffectInstance(previousLevitation);
        }
        try {
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(100);
            for (boolean retainedFlight : new boolean[] {false, true}) {
                BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(retainedFlight);
                player.removeStatusEffect(StatusEffects.LEVITATION);
                if (retainedFlight) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
                }
                for (boolean cancel : new boolean[] {false, true}) {
                    BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.setBooleanValue(cancel);
                    for (boolean customLifetime : new boolean[] {false, true}) {
                        BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(customLifetime);
                        String label = "retainedFlight=" + retainedFlight + ", cancel=" + cancel
                                + ", customLifetime=" + customLifetime;
                        player.setOnGround(false);
                        player.startFallFlying();
                        if (retainedFlight) {
                            clearTrackedFlight(player);
                        }
                        check(player.isFallFlying(), label + ": control glide must be active");
                        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
                        stack.getOrCreateSubNbt("Fireworks").putByte("Flight", (byte) 1);
                        FireworkRocketEntity rocket = new FireworkRocketEntity(player.getWorld(), stack, player);
                        ((ClientFireworkRocket) rocket).bettermoving$markLocalSimulation();
                        ClientFireworkRocketManager.track(rocket);
                        try {
                            player.setVelocity(Vec3d.ZERO);
                            rocket.tick();
                            check(player.getVelocity().lengthSquared() > 0.0 && !rocket.isRemoved(),
                                    label + ": control firework must boost before reset");
                            player.setVelocity(MOMENTUM);
                            check(input(TEST_KEY, true), label + ": press must trigger reset");
                            check(player.getVelocity().equals(Vec3d.ZERO) && !player.isFallFlying(),
                                    label + ": reset must clear velocity and end the glide");
                            check(rocket.isRemoved() == cancel,
                                    label + ": reset must respect the firework cancellation option");
                            if (!cancel) {
                                rocket.tick();
                                check(player.getVelocity().equals(Vec3d.ZERO),
                                        label + ": remaining firework must not boost while gliding is stopped");
                            }
                            LevitationElytraFlight.tick(client);
                            clearTrackedFlight(player);
                            check(!player.isFallFlying(), label + ": stopped glide must not resume without input");
                            check(player.hasStatusEffect(StatusEffects.LEVITATION) == retainedFlight,
                                    label + ": reset must not remove real status effects");
                            player.startFallFlying();
                            check(player.isFallFlying(), label + ": a new glide must still be possible");
                            if (!cancel) {
                                rocket.tick();
                                check(player.getVelocity().lengthSquared() > 0.0,
                                        label + ": retained firework must resume only with a new glide");
                            }
                        } finally {
                            input(TEST_KEY, false);
                            rocket.discard();
                            player.stopFallFlying();
                        }
                    }
                }
            }
        } finally {
            player.stopFallFlying();
            player.removeStatusEffect(StatusEffects.LEVITATION);
            if (previousLevitation != null) {
                player.addStatusEffect(previousLevitation);
            }
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(previousInfinite);
            BetterMovingConfigs.CANCEL_ELYTRA_FIREWORK_ON_STOP.setBooleanValue(previousCancel);
            BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.setBooleanValue(previousLifetime);
            BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.setIntegerValue(previousFlight1Lifetime);
            BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(previousIgnore);
        }
    }

    private static void clearTrackedFlight(ClientPlayerEntity player) {
        var flags = EntityFlagsProbe.bettermovingTest$getFlags();
        byte value = (byte) (player.getDataTracker().get(flags) & ~(1 << 7));
        new EntityTrackerUpdateS2CPacket(player.getId(), List.of(DataTracker.SerializedEntry.of(flags, value)))
                .apply(player.networkHandler);
    }

    private static boolean input(int key, boolean pressed) {
        return ((InputEventHandler) InputEventHandler.getInputManager()).onKeyInput(key, 0, 0, pressed);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
