package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.PotionEffectPolicy;
import fi.dy.masa.malilib.config.ConfigUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class PotionEffectsCompatProbe {
    private static final double TOLERANCE = 1.0E-6;

    private PotionEffectsCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean originalEnabled = BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue();
        int originalSpeedLevel = BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue();
        int originalJumpBoostLevel = BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue();
        StatusEffectInstance originalSpeed = copy(player.getStatusEffect(StatusEffects.SPEED));
        StatusEffectInstance originalJumpBoost = copy(player.getStatusEffect(StatusEffects.JUMP_BOOST));

        try {
            verifyConfiguration();
            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);

            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
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
                            true),
                    player.getMovementSpeed());
            close(
                    "Simulated Jump Boost I",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            baselineJumpBoost,
                            0,
                            1,
                            true),
                    player.getJumpBoostVelocityModifier());

            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(3);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(2);
            close(
                    "Simulated Speed III",
                    PotionEffectPolicy.applySimulatedSpeed(
                            baselineMovementSpeed,
                            0,
                            3,
                            true),
                    player.getMovementSpeed());
            close(
                    "Simulated Jump Boost II",
                    PotionEffectPolicy.applySimulatedJumpBoost(
                            baselineJumpBoost,
                            0,
                            2,
                            true),
                    player.getJumpBoostVelocityModifier());

            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
            close("Disabled simulated speed", baselineMovementSpeed, player.getMovementSpeed());
            close("Disabled simulated jump boost", baselineJumpBoost, player.getJumpBoostVelocityModifier());
            BetterMovingClient.LOGGER.info("Potion effect compatibility checks passed");
        } finally {
            BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(originalEnabled);
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(originalSpeedLevel);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(originalJumpBoostLevel);
            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            restore(player, originalSpeed);
            restore(player, originalJumpBoost);
        }
    }

    private static void verifyConfiguration() {
        check(!BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getDefaultBooleanValue(),
                "Simulate potion effects must default to disabled");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getDefaultIntegerValue() == 0,
                "Simulated speed potion level must default to zero");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getDefaultIntegerValue() == 0,
                "Simulated jump boost level must default to zero");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATE_POTION_EFFECTS),
                "Missing simulated potion effects GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL),
                "Missing simulated speed potion level GUI option");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL),
                "Missing simulated jump boost level GUI option");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATE_POTION_EFFECTS),
                "Simulate potion effects must be registered as a hotkey");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL),
                "Simulated speed potion level must not be registered as a hotkey");
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL),
                "Simulated jump boost level must not be registered as a hotkey");

        JsonObject serialized = new JsonObject();
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(3);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(2);
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(0);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(0);
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue(),
                "Simulated potion effects persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue() == 3,
                "Simulated speed potion level persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue() == 2,
                "Simulated jump boost level persistence was not restored");
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
