package dev.bettermoving.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.bettermoving.compat.RiptideStateProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.entity.ClientFireworkRocket;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
                boolean gliding = player.isFallFlying();
                boolean swimming = player.isSwimming();
                boolean sprinting = player.isSprinting();
                boolean spinning = player.isUsingRiptide();
                Vec3d position = player.getPos();
                player.setVelocity(MOMENTUM);
                check(input(TEST_KEY, true), state + ": press must consume the hotkey");
                check(player.getVelocity().equals(Vec3d.ZERO), state + ": all three axes must reset");
                check(player.getPos().equals(position), state + ": position must not change");
                check(player.fallDistance == 14.0F, state + ": fall distance must not change");
                check(player.isFallFlying() == gliding && player.isSwimming() == swimming
                                && player.isSprinting() == sprinting && player.isUsingRiptide() == spinning,
                        state + ": movement state must not change");
                check(remote.getVelocity().equals(MOMENTUM), state + ": remote players must be unaffected");

                player.setVelocity(MOMENTUM.negate());
                for (int tick = 0; tick < 3; tick++) {
                    TickHandler.getInstance().onClientTick(client);
                    check(!input(TEST_KEY, true), state + ": held/repeated input must not retrigger");
                    check(player.getVelocity().equals(MOMENTUM.negate()), state + ": holding must preserve new motion");
                }
                check(!input(TEST_KEY, false), state + ": release must not trigger");
                check(player.getVelocity().equals(MOMENTUM.negate()), state + ": release must preserve velocity");
            }

            verifyInputContext(client, keybind);
            verifyMotionResumes(player);
            client.player = null;
            check(!keybind.getCallback().onKeyAction(KeyAction.PRESS, keybind),
                    "Missing player must be safely ignored");
            System.out.println("BetterMoving momentum reset compatibility probe passed: press edges, states, context, persistence, and resumed motion");
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
        player.setVelocity(MOMENTUM);
        input(GLFW.GLFW_KEY_W, true);
        check(!input(TEST_KEY, true), "Default settings must reject extra movement keys");
        check(player.getVelocity().equals(MOMENTUM), "Rejected shared input must preserve momentum");
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
        player.setVelocity(MOMENTUM);
        check(!input(TEST_KEY, true), "In-game hotkey must not activate inside a GUI");
        check(player.getVelocity().equals(MOMENTUM), "GUI input must not reset momentum");
        input(TEST_KEY, false);
        client.setScreen(null);
        keybind.setValueFromString("");
        InputEventHandler.getKeybindManager().updateUsedKeys();
        check(!input(TEST_KEY, true), "Empty binding must remain inert");
        check(player.getVelocity().equals(MOMENTUM), "Unbound action must not reset momentum");
        input(TEST_KEY, false);
        keybind.setValueFromString("F8");
        InputEventHandler.getKeybindManager().updateUsedKeys();
        check(!keybind.getCallback().onKeyAction(KeyAction.RELEASE, keybind), "Release callback must be ignored");
        check(player.getVelocity().equals(MOMENTUM), "Release callback must preserve motion");
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

        player.startFallFlying();
        FireworkRocketEntity rocket = new FireworkRocketEntity(player.getWorld(), new ItemStack(Items.FIREWORK_ROCKET), player);
        ((ClientFireworkRocket) rocket).bettermoving$markLocalSimulation();
        try {
            player.setVelocity(MOMENTUM);
            check(input(TEST_KEY, true), "Firework check must trigger reset");
            check(player.getVelocity().equals(Vec3d.ZERO) && player.isFallFlying(),
                    "Reset must clear velocity without stopping the active glide");
            rocket.tick();
            check(player.getVelocity().lengthSquared() > 0.0 && !rocket.isRemoved(),
                    "An active rocket must continue boosting after the single reset");
            input(TEST_KEY, false);
        } finally {
            rocket.discard();
        }
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
