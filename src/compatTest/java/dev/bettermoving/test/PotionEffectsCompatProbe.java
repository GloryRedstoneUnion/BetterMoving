package dev.bettermoving.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.compat.EntityFlagsProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.config.FluidMovementModel;
import dev.bettermoving.physics.LevitationElytraFlight;
import dev.bettermoving.physics.PotionEffectPolicy;
import fi.dy.masa.malilib.config.ConfigUtils;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PotionEffectsCompatProbe {
    private static final double TOLERANCE = 1.0E-6;

    private PotionEffectsCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean originalEnabled = BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue();
        boolean originalOverride = BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.getBooleanValue();
        boolean originalIgnoreFluidPhysics = BetterMovingConfigs.IGNORE_FLUID_PHYSICS.getBooleanValue();
        boolean originalIgnoreMovementEffects = BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.getBooleanValue();
        int originalSpeedLevel = BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue();
        int originalJumpBoostLevel = BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue();
        int originalDolphinsGraceLevel = BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getIntegerValue();
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
            BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(0);
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);

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
            verifyDolphinsGraceMovement(client);

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
            check(
                    PotionEffectPolicy.resolveDolphinsGrace(
                            false,
                            1,
                            BetterMovingConfigs.simulatePotionEffects(),
                            BetterMovingConfigs.overridePotionEffects()),
                    "Simulated Dolphin's Grace I was not applied");

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 200, 2));
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(1);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(1);
            BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(1);
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
            check(
                    PotionEffectPolicy.resolveDolphinsGrace(
                            true,
                            BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getIntegerValue(),
                            true,
                            false),
                    "Existing Dolphin's Grace was not preserved when override is disabled");

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
            check(
                    !PotionEffectPolicy.resolveDolphinsGrace(
                            true,
                            0,
                            true,
                            true),
                    "Dolphin's Grace override did not honor the configured zero level");

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
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(originalIgnoreFluidPhysics);
            BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(originalIgnoreMovementEffects);
            BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(originalSpeedLevel);
            BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(originalJumpBoostLevel);
            BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(originalDolphinsGraceLevel);
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
        check(BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getDefaultIntegerValue() == 0,
                "Simulated Dolphin's Grace level must default to zero");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getMaxIntegerValue() == Integer.MAX_VALUE,
                "Simulated speed potion level must accept the full non-negative Java int range");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getMaxIntegerValue() == Integer.MAX_VALUE,
                "Simulated jump boost level must accept the full non-negative Java int range");
        check(BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getMaxIntegerValue() == Integer.MAX_VALUE,
                "Simulated Dolphin's Grace level must accept the full non-negative Java int range");
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
        check(BetterMovingConfigs.GUI_OPTIONS.contains(BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL),
                "Missing simulated Dolphin's Grace level GUI option");
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
        check(!BetterMovingConfigs.ALL_HOTKEYS.contains(BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL),
                "Simulated Dolphin's Grace level must not be registered as a hotkey");

        JsonObject serialized = new JsonObject();
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(3);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(2);
        BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(1);
        ConfigUtils.writeConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(false);
        BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.setBooleanValue(false);
        BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.setIntegerValue(0);
        BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.setIntegerValue(0);
        BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(0);
        ConfigUtils.readConfigBase(serialized, "options", BetterMovingConfigs.OPTIONS);
        check(BetterMovingConfigs.SIMULATE_POTION_EFFECTS.getBooleanValue(),
                "Simulated potion effects persistence was not restored");
        check(BetterMovingConfigs.OVERRIDE_POTION_EFFECTS.getBooleanValue(),
                "Override potion effects persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_SPEED_POTION_LEVEL.getIntegerValue() == 3,
                "Simulated speed potion level persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_JUMP_BOOST_LEVEL.getIntegerValue() == 2,
                "Simulated jump boost level persistence was not restored");
        check(BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.getIntegerValue() == 1,
                "Simulated Dolphin's Grace level persistence was not restored");
    }

    private static void verifyDolphinsGraceMovement(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos center = player.getBlockPos().add(8, 0, 0);
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 2, 2))) {
            client.world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
        }

        player.setPosition(Vec3d.ofCenter(center));
        player.setSwimming(false);
        player.setOnGround(false);
        player.setNoGravity(true);
        player.setSprinting(false);
        player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        player.baseTick();
        boolean movementInWater = player.updateMovementInFluid(FluidTags.WATER, 0.0);
        if (!movementInWater || !player.isTouchingWater()) {
            throw new AssertionError(
                    "The potion effects compatibility probe did not place the player in water: "
                            + "centerFluid=" + client.world.getFluidState(center)
                            + ", movementInWater=" + movementInWater
                            + ", touchingWater=" + player.isTouchingWater()
                            + ", waterHeight=" + player.getFluidHeight(FluidTags.WATER)
                            + ", playerPos=" + player.getPos());
        }

        player.setVelocity(0.25, 0.0, 0.0);
        player.travel(Vec3d.ZERO);
        close("Vanilla water drag without Dolphin's Grace", 0.20, player.getVelocity().x);

        BetterMovingConfigs.SIMULATE_POTION_EFFECTS.setBooleanValue(true);
        BetterMovingConfigs.SIMULATED_DOLPHINS_GRACE_LEVEL.setIntegerValue(1);
        player.setPosition(Vec3d.ofCenter(center));
        player.setVelocity(0.25, 0.0, 0.0);
        player.updateMovementInFluid(FluidTags.WATER, 0.0);
        player.travel(Vec3d.ZERO);
        close("Simulated Dolphin's Grace water drag", 0.24, player.getVelocity().x);
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
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        // Refresh the cached fluid state after leaving the water fixture.  A position
        // change alone does not clear LivingEntity's touchingWater flag, which would
        // send the vanilla levitation check through the water movement branch.
        player.baseTick();
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
        player.travel(Vec3d.ZERO);
        check(player.getVelocity().y > 0.0,
                "Levitation did not provide its vanilla upward movement: velocity="
                        + player.getVelocity()
                        + ", hasEffect=" + player.hasStatusEffect(StatusEffects.LEVITATION)
                        + ", logicalSide=" + player.isLogicalSideForUpdatingMovement()
                        + ", position=" + player.getPos());

        player.setVelocity(Vec3d.ZERO);
        player.setNoGravity(false);
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
        verifyLevitationElytraInput(player);
    }

    private static void verifyLevitationElytraInput(ClientPlayerEntity player) {
        Input originalInput = player.input;
        boolean originalAllowFlying = player.getAbilities().allowFlying;
        boolean originalFlying = player.getAbilities().flying;
        TrackedData<Byte> flags = EntityFlagsProbe.bettermovingTest$getFlags();
        byte originalFlags = player.getDataTracker().get(flags);
        try {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.input = new Input() {
                @Override
                public void tick(boolean slowDown, float slowDownFactor) {
                    this.jumping = true;
                }
            };
            player.setOnGround(false);
            player.setVelocity(0.0, -0.1, 0.0);
            player.tick();
            check(player.isFallFlying(),
                    "Ignored Levitation blocked Elytra activation through the real jump-input path");
            serverLevitationRejection(player).apply(player.networkHandler);
            check(player.isFallFlying(),
                    "Server metadata interrupted local Elytra flight while Levitation was ignored");
            check((player.getDataTracker().get(flags) & (1 << 7)) == 0,
                    "Local flight override changed server-synchronized metadata");
            player.input = new Input();
            for (int tick = 0; tick < 20; ++tick) {
                clearServerFlight(player);
                player.tick();
                check(player.isFallFlying(),
                        "Ignored Levitation interrupted Elytra flight at tick " + tick);
                check(player.getPose() == EntityPose.FALL_FLYING,
                        "Ignored Levitation lost the Elytra pose at tick " + tick);
            }
            check(player.hasStatusEffect(StatusEffects.LEVITATION),
                    "Elytra movement override removed the real Levitation effect");
            verifyLevitationElytraBoundaries(player);
        } finally {
            player.stopFallFlying();
            player.getDataTracker().set(flags, (byte) (originalFlags & ~(1 << 7)));
            player.input = originalInput;
            player.getAbilities().allowFlying = originalAllowFlying;
            player.getAbilities().flying = originalFlying;
        }
    }

    private static void verifyLevitationElytraBoundaries(ClientPlayerEntity player) {
        check(!BetterMovingConfigs.simulatePotionEffects(),
                "Levitation Elytra regression must not depend on potion simulation");
        clearServerFlight(player);
        byte originalFlags = player.getDataTracker().get(EntityFlagsProbe.bettermovingTest$getFlags());
        syncFlags(player, (byte) (originalFlags | (1 << 6)));
        check(player.isGlowing() && player.isFallFlying(),
                "Preserving local Elytra flight discarded unrelated metadata flags");
        syncFlags(player, originalFlags);

        OtherClientPlayerEntity remote = new OtherClientPlayerEntity(player.clientWorld,
                new GameProfile(UUID.randomUUID(), "ElytraRegressionRemote"));
        remote.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        remote.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
        remote.setOnGround(false);
        remote.startFallFlying();
        remote.getDataTracker().set(EntityFlagsProbe.bettermovingTest$getFlags(), (byte) 0);
        check(!remote.isFallFlying() && player.isFallFlying(),
                "Local Elytra override leaked to another player or lost its own session");

        verifyLevitationFireworks(player);
        assertFlightStops(player, "explicit stop", player::stopFallFlying, () -> { });
        assertFlightStops(player, "landing",
                () -> player.setOnGround(true), () -> player.setOnGround(false));
        assertFlightStops(player, "disabled override",
                () -> BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false),
                () -> BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(true));
        assertFlightStops(player, "effect removal",
                () -> player.removeStatusEffect(StatusEffects.LEVITATION),
                () -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0)));
        assertFlightStops(player, "missing Elytra",
                () -> player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY),
                () -> player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA)));
        ItemStack brokenElytra = new ItemStack(Items.ELYTRA);
        brokenElytra.setDamage(brokenElytra.getMaxDamage() - 1);
        assertFlightStops(player, "unusable Elytra",
                () -> player.equipStack(EquipmentSlot.CHEST, brokenElytra),
                () -> player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA)));
        assertFlightStops(player, "creative flight",
                () -> player.getAbilities().flying = true,
                () -> player.getAbilities().flying = false);
        var vehicle = EntityType.PIG.create(player.getWorld());
        assertFlightStops(player, "riding",
                () -> check(player.startRiding(vehicle, true), "Could not mount the test vehicle"),
                player::stopRiding);
        float health = player.getHealth();
        assertFlightStops(player, "death", () -> player.setHealth(0.0F), () -> player.setHealth(health));

        MinecraftClient client = MinecraftClient.getInstance();
        assertFlightStops(player, "disconnect",
                () -> {
                    client.player = null;
                    LevitationElytraFlight.tick(client);
                    client.player = player;
                }, () -> { });

        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(false);
        check(!player.checkFallFlying(), "Disabled override allowed Levitation Elytra activation");
        BetterMovingConfigs.IGNORE_LEVITATION_AND_SLOWNESS.setBooleanValue(true);
        player.removeStatusEffect(StatusEffects.LEVITATION);
        check(player.checkFallFlying(), "Ordinary Elytra activation was broken");
        clearServerFlight(player);
        check(!player.isFallFlying(), "Non-Levitation server flight resets were ignored");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
        check(!player.isFallFlying(), "Adding Levitation started an unrequested Elytra flight");

        player.removeStatusEffect(StatusEffects.LEVITATION);
        check(player.checkFallFlying(), "Ordinary flight could not start before receiving Levitation");
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
        clearServerFlight(player);
        check(player.isFallFlying(), "Receiving Levitation interrupted an existing Elytra flight");
        player.stopFallFlying();
        verifyLevitationFluidModes(player);
        BetterMovingClient.LOGGER.info("Levitation Elytra activation, metadata, stopping, fluid and firework checks passed");
    }

    private static void assertFlightStops(
            ClientPlayerEntity player, String label, Runnable stopCondition, Runnable restore) {
        player.stopFallFlying();
        check(player.checkFallFlying(), label + ": could not start the control flight");
        clearServerFlight(player);
        check(player.isFallFlying(), label + ": could not retain the control flight");
        try {
            stopCondition.run();
            check(!player.isFallFlying(), label + ": local flight was not stopped");
        } finally {
            restore.run();
        }
        check(!player.isFallFlying(), label + ": local flight resumed without new input");
    }

    private static void verifyLevitationFireworks(ClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack originalHand = player.getMainHandStack();
        boolean originalInfinite = BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue();
        FireworkRocketEntity rocket = null;
        try {
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);
            check(client.interactionManager.interactItem(player, Hand.MAIN_HAND).isAccepted(),
                    "Local Elytra flight with Levitation did not accept a firework boost");
            check(player.getMainHandStack().getCount() == 3,
                    "Infinite Levitation Elytra boost consumed a rocket");
            rocket = client.world.getEntitiesByType(TypeFilter.instanceOf(FireworkRocketEntity.class),
                            player.getBoundingBox().expand(2.0), entity -> true)
                    .stream().findFirst().orElseThrow(() -> new AssertionError("No Levitation Elytra boost rocket"));
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            player.setVelocity(Vec3d.ZERO);
            rocket.tick();
            check(player.getVelocity().z > 0.0, "Levitation Elytra firework did not accelerate the player");
        } finally {
            if (rocket != null) {
                rocket.discard();
            }
            player.setStackInHand(Hand.MAIN_HAND, originalHand);
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(originalInfinite);
        }
    }

    private static void verifyLevitationFluidModes(ClientPlayerEntity player) {
        boolean originalFluidToggle = BetterMovingConfigs.ignoreFluidPhysics();
        FluidMovementModel originalModel = BetterMovingConfigs.movementModel();
        Vec3d originalPosition = player.getPos();
        BlockPos center = player.getBlockPos().up(10);
        try {
            for (BlockPos pos : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
                player.getWorld().setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
            }
            player.setPosition(Vec3d.ofCenter(center));
            player.baseTick();
            player.setOnGround(false);
            check(player.isTouchingWater(), "Levitation fluid fixture did not contain water");
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
            check(!player.checkFallFlying(), "Ordinary water allowed Levitation Elytra activation");
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(true);
            BetterMovingConfigs.MODEL.setOptionListValue(FluidMovementModel.AIR);
            check(player.checkFallFlying(), "Air rules did not allow Levitation Elytra activation in water");
            clearServerFlight(player);
            check(player.isFallFlying(), "Air rules did not preserve Levitation Elytra flight in water");
            BetterMovingConfigs.MODEL.setOptionListValue(FluidMovementModel.WATER);
            check(!player.isFallFlying(), "Water rules did not stop local Levitation Elytra flight");
            check(!player.checkFallFlying(), "Water rules incorrectly allowed Levitation Elytra activation");
        } finally {
            player.stopFallFlying();
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(originalFluidToggle);
            BetterMovingConfigs.MODEL.setOptionListValue(originalModel);
            player.setPosition(originalPosition);
            player.baseTick();
        }
    }

    private static void clearServerFlight(ClientPlayerEntity player) {
        byte flags = player.getDataTracker().get(EntityFlagsProbe.bettermovingTest$getFlags());
        syncFlags(player, (byte) (flags & ~(1 << 7)));
    }

    private static void syncFlags(ClientPlayerEntity player, byte value) {
        new EntityTrackerUpdateS2CPacket(player.getId(), List.of(DataTracker.SerializedEntry.of(
                EntityFlagsProbe.bettermovingTest$getFlags(), value))).apply(player.networkHandler);
    }

    private static EntityTrackerUpdateS2CPacket serverLevitationRejection(ClientPlayerEntity player) {
        try {
            return MinecraftClient.getInstance().getServer().submit(() -> {
                ServerPlayerEntity serverPlayer = MinecraftClient.getInstance().getServer()
                        .getPlayerManager().getPlayer(player.getUuid());
                ItemStack originalChest = serverPlayer.getEquippedStack(EquipmentSlot.CHEST).copy();
                StatusEffectInstance originalEffect = copy(serverPlayer.getStatusEffect(StatusEffects.LEVITATION));
                boolean originalGround = serverPlayer.isOnGround();
                boolean originalFlight = serverPlayer.isFallFlying();
                try {
                    serverPlayer.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
                    serverPlayer.setOnGround(false);
                    serverPlayer.removeStatusEffect(StatusEffects.LEVITATION);
                    serverPlayer.stopFallFlying();
                    check(serverPlayer.checkFallFlying(),
                            "Server Elytra control failed without Levitation");
                    serverPlayer.stopFallFlying();
                    serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 0));
                    serverPlayer.getDataTracker().getDirtyEntries();
                    serverPlayer.networkHandler.onClientCommand(new ClientCommandC2SPacket(
                            serverPlayer, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    check(!serverPlayer.isFallFlying(),
                            "Client override unexpectedly changed server Levitation behavior");
                    List<DataTracker.SerializedEntry<?>> updates = serverPlayer.getDataTracker().getDirtyEntries();
                    check(updates != null && updates.stream().anyMatch(entry ->
                                    entry.id() == EntityFlagsProbe.bettermovingTest$getFlags().getId()),
                            "Server did not produce the rejected-flight flag update");
                    return new EntityTrackerUpdateS2CPacket(serverPlayer.getId(), updates);
                } finally {
                    serverPlayer.removeStatusEffect(StatusEffects.LEVITATION);
                    if (originalEffect != null) {
                        serverPlayer.addStatusEffect(originalEffect);
                    }
                    serverPlayer.equipStack(EquipmentSlot.CHEST, originalChest);
                    serverPlayer.setOnGround(originalGround);
                    if (originalFlight) {
                        serverPlayer.startFallFlying();
                    } else {
                        serverPlayer.stopFallFlying();
                    }
                }
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("Failed to reproduce vanilla server Elytra rejection", exception);
        }
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
