package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.PotionEffectPolicy;
import fi.dy.masa.malilib.config.ConfigUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public final class PotionEffectsCompatProbe {
    private static final double TOLERANCE = 1.0E-6;

    private PotionEffectsCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean originalEnabled = BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue();
        boolean originalOverride = BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.getBooleanValue();
        boolean originalIgnoreMovementEffects = BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.getBooleanValue();
        int originalSpeedLevel = BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue();
        int originalJumpBoostLevel = BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue();
        StatusEffectInstance originalSpeed = copy(player.getStatusEffect(StatusEffects.SPEED));
        StatusEffectInstance originalJumpBoost = copy(player.getStatusEffect(StatusEffects.JUMP_BOOST));
        StatusEffectInstance originalSlowness = copy(player.getStatusEffect(StatusEffects.SLOWNESS));
        StatusEffectInstance originalLevitation = copy(player.getStatusEffect(StatusEffects.LEVITATION));
        Vec3d originalPosition = player.getPos();
        Vec3d originalVelocity = player.getVelocity();
        ItemStack originalChest = player.getEquippedStack(EquipmentSlot.CHEST).copy();
        boolean originalNoGravity = player.hasNoGravity();
        boolean originalOnGround = player.isOnGround();

        try {
            verifyConfiguration();
            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            player.removeStatusEffect(StatusEffects.SLOWNESS);
            player.removeStatusEffect(StatusEffects.LEVITATION);

            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
            BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(false);
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(0);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(0);

            float baselineMovementSpeed = player.getMovementSpeed();
            float baselineJumpBoost = player.getJumpBoostVelocityModifier();

            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(true);
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(1);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(1);
            close(
                    "Simulated Speed I",
                    PotionEffectPolicy.applySimulatedSpeed(
                            baselineMovementSpeed,
                            0,
                            1,
                            true,
                            false),
                    player.getMovementSpeed());
            close(
                    "Simulated Jump Boost I",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            baselineJumpBoost,
                            0,
                            1,
                            true,
                            false),
                    player.getJumpBoostVelocityModifier());

            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(3);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(2);
            close(
                    "Simulated Speed III",
                    PotionEffectPolicy.applySimulatedSpeed(
                            baselineMovementSpeed,
                            0,
                            3,
                            true,
                            false),
                    player.getMovementSpeed());
            close(
                    "Simulated Jump Boost II",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            baselineJumpBoost,
                            0,
                            2,
                            true,
                            false),
                    player.getJumpBoostVelocityModifier());

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 200, 2));
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(1);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(1);
            BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(false);
            float existingMovementSpeed = player.getMovementSpeed();
            float existingJumpBoost = player.getJumpBoostVelocityModifier();
            close(
                    "Existing Speed III is preserved when override is disabled",
                    PotionEffectPolicy.applySimulatedSpeed(
                            existingMovementSpeed,
                            3,
                            1,
                            true,
                            false),
                    player.getMovementSpeed());
            close(
                    "Existing Jump Boost III is preserved when override is disabled",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            existingJumpBoost,
                            3,
                            1,
                            true,
                            false),
                    player.getJumpBoostVelocityModifier());

            BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(true);
            close(
                    "Existing Speed III is replaced when override is enabled",
                    PotionEffectPolicy.applySimulatedSpeed(
                            existingMovementSpeed,
                            3,
                            1,
                            true,
                            true),
                    player.getMovementSpeed());
            close(
                    "Existing Jump Boost III is replaced when override is enabled",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            existingJumpBoost,
                            3,
                            1,
                            true,
                            true),
                    player.getJumpBoostVelocityModifier());

            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
            close("Disabled simulated speed", baselineMovementSpeed, player.getMovementSpeed());
            close("Disabled simulated jump boost", baselineJumpBoost, player.getJumpBoostVelocityModifier());
            verifyMovementEffectOverride(player, baselineMovementSpeed);
            BetterMovingClient.LOGGER.info("Potion effect compatibility checks passed");
        } finally {
            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(originalEnabled);
            BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(originalOverride);
            BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(originalIgnoreMovementEffects);
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(originalSpeedLevel);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(originalJumpBoostLevel);
            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            player.removeStatusEffect(StatusEffects.SLOWNESS);
            player.removeStatusEffect(StatusEffects.LEVITATION);
            restore(player, originalSpeed);
            restore(player, originalJumpBoost);
            restore(player, originalSlowness);
            restore(player, originalLevitation);
            player.equipStack(EquipmentSlot.CHEST, originalChest);
            player.setPosition(originalPosition);
            player.setVelocity(originalVelocity);
            player.setNoGravity(originalNoGravity);
            player.setOnGround(originalOnGround);
        }
    }

    private static void verifyConfiguration() {
        check(!BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getDefaultBooleanValue(),
                "Simulate potion effects must default to disabled");
        check(!BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.getDefaultBooleanValue(),
                "Override potion effects must default to disabled");
        check(!BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.getDefaultBooleanValue(),
                "Levitation and Slowness override must default to disabled");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getDefaultIntegerValue() == 0,
                "Simulated speed potion level must default to zero");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getDefaultIntegerValue() == 0,
                "Simulated jump boost level must default to zero");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATE_POTION_EFFECTS),
                "Missing simulated potion effects GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.OVERRIDE_POTION_EFFECTS),
                "Missing override potion effects GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS),
                "Missing Levitation and Slowness GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL),
                "Missing simulated speed potion level GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL),
                "Missing simulated jump boost level GUI option");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATE_POTION_EFFECTS),
                "Simulate potion effects must be registered as a hotkey");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.OVERRIDE_POTION_EFFECTS),
                "Override potion effects must be registered as a hotkey");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS),
                "Levitation and Slowness override must be registered as a hotkey");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL),
                "Simulated speed potion level must not be registered as a hotkey");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL),
                "Simulated jump boost level must not be registered as a hotkey");

        JsonObject serialized = new JsonObject();
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(3);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(2);
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
        BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(false);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(0);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(0);
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue(),
                "Simulated potion effects persistence was not restored");
        check(BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.getBooleanValue(),
                "Override potion effects persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue() == 3,
                "Simulated speed potion level persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue() == 2,
                "Simulated jump boost level persistence was not restored");
    }

    private static void verifyMovementEffectOverride(
            ClientPlayerEntity player,
            float baselineMovementSpeed) {
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0));
        applySlownessAttributeModifier(player, 0);
        float slowedMovementSpeed = player.getMovementSpeed();
        check(slowedMovementSpeed < baselineMovementSpeed,
                "Slowness did not reduce the baseline movement speed");

        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(true);
        close("Ignored Slowness movement speed", baselineMovementSpeed, player.getMovementSpeed());
        check(player.hasStatusEffect(StatusEffects.SLOWNESS),
                "Ignoring Slowness removed the real status effect");

        removeSlownessAttributeModifier(player, 0);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
        player.setPosition(player.getX(), 200.0, player.getZ());
        player.setOnGround(false);
        player.setNoGravity(false);
        player.setVelocity(Vec3d.ZERO);
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
        player.travel(Vec3d.ZERO);
        check(player.getVelocity().y > 0.0,
                "Levitation did not provide its vanilla upward movement");

        player.setVelocity(Vec3d.ZERO);
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(true);
        player.travel(Vec3d.ZERO);
        check(player.getVelocity().y < 0.0,
                "Ignored Levitation did not restore ordinary gravity");
        check(player.hasStatusEffect(StatusEffects.LEVITATION),
                "Ignoring Levitation removed the real status effect");

        player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.stopFallFlying();
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
        check(!player.checkFallFlying(),
                "Levitation did not block vanilla Elytra activation");
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(true);
        check(player.checkFallFlying(),
                "Ignored Levitation did not allow Elytra activation");
        player.stopFallFlying();
    }

    private static void applySlownessAttributeModifier(ClientPlayerEntity player, int amplifier) {
        StatusEffects.SLOWNESS.onApplied(player, player.getAttributes(), amplifier);
    }

    private static void removeSlownessAttributeModifier(ClientPlayerEntity player, int amplifier) {
        StatusEffects.SLOWNESS.onRemoved(player, player.getAttributes(), amplifier);
    }

    private static StatusEffectInstance copy(StatusEffectInstance effect) {
        return effect == null ? null : new StatusEffectInstance(effect);
    }

    private static void restore(ClientPlayerEntity player, StatusEffectInstance effect) {
        if (effect != null) {
            player.addStatusEffect(new StatusEffectInstance(effect));
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
