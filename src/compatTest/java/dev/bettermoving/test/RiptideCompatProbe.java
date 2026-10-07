package dev.bettermoving.test;

import com.google.gson.JsonObject;
import dev.bettermoving.compat.ClientWorldPredictionProbe;
import dev.bettermoving.compat.RiptideStateProbe;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.config.FluidMovementModel;
import dev.bettermoving.physics.ClientRiptide;
import fi.dy.masa.malilib.config.ConfigUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class RiptideCompatProbe {
    private static boolean capturePackets;
    private static int itemUsePackets;
    private static int releasePackets;
    private static int blockUsePackets;

    private RiptideCompatProbe() {
    }

    public static void observePacket(Packet<?> packet) {
        if (!capturePackets) {
            return;
        }
        if (packet instanceof PlayerInteractItemC2SPacket) {
            itemUsePackets++;
        } else if (packet instanceof PlayerInteractBlockC2SPacket) {
            blockUsePackets++;
        } else if (packet instanceof PlayerActionC2SPacket action
                && action.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            releasePackets++;
        }
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ItemStack previousMain = player.getMainHandStack();
        ItemStack previousOff = player.getOffHandStack();
        Vec3d previousPos = player.getPos();
        boolean previousToggle = BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.getBooleanValue();
        boolean previousCustomCharge = BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.getBooleanValue();
        int previousChargeTime = BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.getIntegerValue();
        boolean previousFluidToggle = BetterMovingConfigs.ignoreFluidPhysics();
        FluidMovementModel previousModel = BetterMovingConfigs.movementModel();
        GameMode previousGameMode = client.interactionManager.getCurrentGameMode();
        try {
            verifyConfiguration();
            capturePackets = true;
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
            BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);
            BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(false);
            // Keep the probe far from terrain and entities without changing server rules.
            player.setPosition(previousPos.x, 240.0, previousPos.z);
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            player.setOnGround(false);
            player.stopFallFlying();
            player.clearActiveItem();
            player.setVelocity(Vec3d.ZERO);
            player.updateMovementInFluid(FluidTags.WATER, 0.0);
            check(!player.isTouchingWaterOrRain(), "Air probe must be dry");

            for (GameMode mode : new GameMode[] {GameMode.SURVIVAL, GameMode.CREATIVE}) {
                client.interactionManager.setGameMode(mode);
                for (Hand hand : Hand.values()) {
                    for (int level = 1; level <= 3; level++) {
                        ItemStack trident = equipTrident(player, hand, level);
                        startCharge(client, hand);
                        verifyMetadataDoesNotCancelCharge(player);
                        charge(player, 10);
                        player.setVelocity(0.25, 0.0, 0.5);
                        client.interactionManager.stopUsingItem(player);
                        assertClose(0.25, player.getVelocity().x, "Riptide keeps existing X velocity");
                        assertClose(0.0, player.getVelocity().y, "Horizontal Riptide adds no Y velocity");
                        assertClose(0.5 + 0.75 * (1 + level), player.getVelocity().z,
                                "Riptide must use the vanilla level-dependent impulse");
                        check(player.isUsingRiptide(), "Local Riptide must expose its spin state");
                        check(!player.isUsingItem() && !ClientRiptide.isCharging(player),
                                "Release must end the charge session");
                        check(trident.getDamage() == 0 && trident.getCount() == 1,
                                "Simulation must preserve the trident");
                        assertPackets(0, 0, 0, "Simulated air Riptide");

                        // Exercise vanilla duration decrement and collision termination.
                        ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(1);
                        player.tickMovement();
                        check(!player.isUsingRiptide(), "Spin must end when vanilla duration expires");
                        player.setPosition(previousPos.x, 240.0, previousPos.z);
                        player.setOnGround(false);
                        ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(20);
                        player.horizontalCollision = true;
                        ((RiptideStateProbe) player).bettermovingTest$tickRiptide(
                                player.getBoundingBox(), player.getBoundingBox());
                        check(!player.isUsingRiptide(), "Wall collision must end simulated spin");
                        player.horizontalCollision = false;
                    }
                }
            }

            verifyShortChargeAndGroundLift(client);
            verifyNaturalTicksAndPose(client);
            verifyCustomChargeTime(client);
            verifyCancellation(client);
            verifyWaterAndFluidModels(client);
            verifyBlockAndEntityInteractions(client);
            verifyVanillaIsolation(client);
            System.out.println("BetterMoving Riptide compatibility probe passed");
        } finally {
            capturePackets = false;
            player.clearActiveItem();
            ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
            player.setStackInHand(Hand.MAIN_HAND, previousMain);
            player.setStackInHand(Hand.OFF_HAND, previousOff);
            player.setPosition(previousPos);
            player.setVelocity(Vec3d.ZERO);
            player.horizontalCollision = false;
            player.getItemCooldownManager().remove(Items.TRIDENT);
            BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(previousToggle);
            BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(previousCustomCharge);
            BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(previousChargeTime);
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(previousFluidToggle);
            BetterMovingConfigs.MODEL.setOptionListValue(previousModel);
            client.interactionManager.setGameMode(previousGameMode);
            ClientRiptide.tick(client);
        }
    }

    private static void verifyConfiguration() {
        var option = BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE;
        check(!option.getDefaultBooleanValue(), "Riptide toggle must default off");
        check(option.getKeybind().getStringValue().isEmpty(), "Riptide hotkey must default unbound");
        check(BetterMovingConfigs.GUI_OPTIONS.contains(option), "Missing Riptide GUI toggle");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(option), "Missing Riptide hotkey");
        JsonObject json = new JsonObject();
        option.setBooleanValue(true);
        ConfigUtils.writeConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        option.setBooleanValue(false);
        ConfigUtils.readConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        check(option.getBooleanValue(), "Riptide toggle did not survive config serialization");

        var custom = BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME;
        var duration = BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS;
        check(!custom.getDefaultBooleanValue(), "Custom charge toggle must default off");
        check(custom.getKeybind().getStringValue().isEmpty(), "Custom charge hotkey must default unbound");
        check(duration.getDefaultIntegerValue() == 10, "Custom charge duration must default to ten ticks");
        check(BetterMovingConfigs.ALL_HOTKEYS.contains(custom), "Missing custom charge hotkey");
        check(BetterMovingConfigs.GUI_OPTIONS.indexOf(custom)
                        == BetterMovingConfigs.GUI_OPTIONS.indexOf(option) + 1,
                "Custom charge toggle must appear below Riptide simulation");
        check(BetterMovingConfigs.GUI_OPTIONS.indexOf(duration)
                        == BetterMovingConfigs.GUI_OPTIONS.size() - 1,
                "Charge duration must be the final GUI option");
        custom.setBooleanValue(true);
        duration.setIntegerValue(Integer.MAX_VALUE);
        check(duration.getIntegerValue() == Integer.MAX_VALUE, "Full int range must be accepted");
        ConfigUtils.writeConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        custom.setBooleanValue(false);
        duration.setIntegerValue(10);
        ConfigUtils.readConfigBase(json, "options", BetterMovingConfigs.OPTIONS);
        check(custom.getBooleanValue() && duration.getIntegerValue() == Integer.MAX_VALUE,
                "Custom charge settings must survive serialization at the int maximum");
        duration.setIntegerValue(-1);
        check(duration.getIntegerValue() == 0, "Negative charge duration must clamp to zero");
        duration.setIntegerValue(10);
        custom.setBooleanValue(false);
    }

    private static void verifyCustomChargeTime(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(true);
        for (GameMode mode : new GameMode[] {GameMode.SURVIVAL, GameMode.CREATIVE}) {
            client.interactionManager.setGameMode(mode);
            for (Hand hand : Hand.values()) {
                for (int required : new int[] {0, 1, 5, 10, 20, 72001, Integer.MAX_VALUE}) {
                    BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(required);
                    if (required > 0) {
                        verifyCustomRelease(client, hand, required - 1, false);
                    }
                    verifyCustomRelease(client, hand, required, true);
                    if (required < Integer.MAX_VALUE) {
                        verifyCustomRelease(client, hand, required + 1, true);
                    }
                }
            }
        }

        // Reach the int boundary using the real vanilla countdown, then keep holding.
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(Integer.MAX_VALUE);
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        setElapsedCharge(player, Integer.MAX_VALUE - 1);
        for (int i = 0; i < 3; i++) {
            ((RiptideStateProbe) player).bettermovingTest$tickActiveItemStack();
            check(player.getItemUseTime() == Integer.MAX_VALUE,
                    "Charge timer must reach and stay at the int maximum without overflow");
        }
        player.setOnGround(false);
        player.setVelocity(Vec3d.ZERO);
        client.interactionManager.stopUsingItem(player);
        assertClose(3.0, player.getVelocity().z, "Maximum-duration charge must still launch");
        assertPackets(0, 0, 0, "Maximum-duration charge");

        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(0);
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(false);
        verifyCustomRelease(client, Hand.MAIN_HAND, 9, false);
        verifyCustomRelease(client, Hand.MAIN_HAND, 10, true);
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(true);

        // Settings are resolved when releasing an already active local charge.
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(20);
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 5);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(5);
        player.setVelocity(Vec3d.ZERO);
        client.interactionManager.stopUsingItem(player);
        assertClose(3.0, player.getVelocity().z, "Updated duration must apply to the active charge");
        assertPackets(0, 0, 0, "Duration change during charge");

        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(0);
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 5);
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(false);
        player.setVelocity(Vec3d.ZERO);
        client.interactionManager.stopUsingItem(player);
        assertClose(0.0, player.getVelocity().length(), "Disabling custom charge restores ten ticks");
        assertPackets(0, 0, 0, "Custom toggle disabled during charge");
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(true);
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        player.setVelocity(Vec3d.ZERO);
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(false);
        client.interactionManager.stopUsingItem(player);
        assertClose(0.0, player.getVelocity().length(), "Disabled simulation must cancel even zero-charge use");
        assertPackets(0, 0, 0, "Parent toggle disabled during custom charge");
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(false);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(10);
    }

    private static void verifyCustomRelease(MinecraftClient client, Hand hand, int elapsed, boolean launch) {
        ClientPlayerEntity player = client.player;
        ((RiptideStateProbe) player).bettermovingTest$setRiptideTicks(0);
        ItemStack trident = equipTrident(player, hand, 3);
        startCharge(client, hand);
        if (elapsed <= 21) {
            charge(player, elapsed);
        } else {
            // Set up long-duration boundaries without waiting days or years.
            setElapsedCharge(player, elapsed);
        }
        player.setOnGround(false);
        player.setVelocity(0.25, 0.0, 0.5);
        client.interactionManager.stopUsingItem(player);
        assertClose(0.25, player.getVelocity().x, "Custom charge preserves existing X motion");
        assertClose(0.0, player.getVelocity().y, "Custom charge preserves vanilla Y impulse");
        assertClose(launch ? 3.5 : 0.5, player.getVelocity().z,
                "Custom threshold launch=" + launch + ", elapsed=" + elapsed);
        check(player.isUsingRiptide() == launch, "Custom threshold must control the vanilla spin");
        check(!player.isUsingItem() && !ClientRiptide.isCharging(player), "Custom release must clear its session");
        check(trident.getDamage() == 0 && trident.getCount() == 1, "Custom release preserves durability");
        assertPackets(0, 0, 0, "Custom charge threshold");
    }

    private static void setElapsedCharge(ClientPlayerEntity player, int ticks) {
        ((RiptideStateProbe) player).bettermovingTest$setItemUseTimeLeft(
                player.getActiveItem().getMaxUseTime() - ticks);
        check(player.getItemUseTime() == ticks, "Long-duration test fixture must use the exact elapsed time");
    }

    private static void verifyShortChargeAndGroundLift(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 9);
        player.setVelocity(Vec3d.ZERO);
        client.interactionManager.stopUsingItem(player);
        assertClose(0.0, player.getVelocity().length(), "Nine-tick charge must not launch");
        assertPackets(0, 0, 0, "Short charge");

        double beforeY = player.getY();
        player.setOnGround(true);
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 10);
        client.interactionManager.stopUsingItem(player);
        assertClose(beforeY + 1.1999999284744263, player.getY(), "Vanilla ground lift");
        assertPackets(0, 0, 0, "Ground launch");
        player.setOnGround(false);
    }

    private static void verifyCancellation(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        equipTrident(player, Hand.MAIN_HAND, 3);
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 10);
        player.setVelocity(Vec3d.ZERO);
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(false);
        client.interactionManager.stopUsingItem(player);
        assertPackets(0, 0, 0, "Disable before release must not leak a release packet");
        assertClose(0.0, player.getVelocity().length(), "Disabled charge must not launch");
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);
        startCharge(client, Hand.MAIN_HAND);
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STONE));
        ClientRiptide.tick(client);
        check(!player.isUsingItem() && !ClientRiptide.isCharging(player),
                "Changing the held item must cancel local use");
        assertPackets(0, 0, 0, "Item switch");

        ItemStack trident = equipTrident(player, Hand.MAIN_HAND, 1);
        player.getItemCooldownManager().set(Items.TRIDENT, 20);
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND) == ActionResult.PASS,
                "Item cooldown must be preserved");
        check(!player.isUsingItem(), "Cooling trident must not start a charge");
        player.getItemCooldownManager().remove(Items.TRIDENT);
        trident.setDamage(trident.getMaxDamage() - 1);
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND) == ActionResult.FAIL,
                "Vanilla unusable-durability check must be preserved");
        assertPackets(0, 0, 0, "Cooldown and durability rejection");
    }

    private static void verifyNaturalTicksAndPose(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        Vec3d position = player.getPos();
        client.interactionManager.setGameMode(GameMode.SURVIVAL);
        for (Hand hand : Hand.values()) {
            player.setPosition(position);
            player.setOnGround(false);
            player.setSwimming(false);
            player.setVelocity(Vec3d.ZERO);
            equipTrident(player, hand, 3);
            startCharge(client, hand);
            for (int tick = 1; tick <= 10; tick++) {
                player.tick();
                check(player.isUsingItem() && player.getItemUseTime() == tick,
                        "Natural player ticks must preserve and advance the local charge");
            }
            verifyMetadataDoesNotCancelCharge(player);
            client.interactionManager.stopUsingItem(player);
            for (int tick = 1; tick <= 20; tick++) {
                player.tick();
                check(player.isUsingRiptide() == (tick < 20),
                        "Natural ticks must keep the spin for its vanilla duration");
                check(player.isInPose(EntityPose.SPIN_ATTACK) == (tick < 20),
                        "Vanilla pose must follow the locally simulated spin");
            }
            assertPackets(0, 0, 0, "Natural Riptide ticks");
        }
        player.setPosition(position);
        player.setVelocity(Vec3d.ZERO);
    }

    private static void verifyBlockAndEntityInteractions(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos target = player.getBlockPos().down();
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(target), Direction.UP, target, false);
        equipTrident(player, Hand.MAIN_HAND, 3);
        equipTrident(player, Hand.OFF_HAND, 3);
        for (GameMode mode : new GameMode[] {GameMode.SURVIVAL, GameMode.CREATIVE}) {
            client.interactionManager.setGameMode(mode);
            for (Hand hand : Hand.values()) {
                for (Block block : new Block[] {Blocks.CRAFTING_TABLE, Blocks.CHEST, Blocks.BARREL, Blocks.LEVER}) {
                    client.world.setBlockState(target, block.getDefaultState());
                    resetPackets();
                    check(client.interactionManager.interactBlock(player, hand, hit).isAccepted(),
                            "Riptide simulation must preserve accepted block interactions: " + block);
                    assertPackets(0, 0, 1, "Accepted block interaction");
                    check(!((ClientWorldPredictionProbe) client.world)
                                    .bettermovingTest$getPendingUpdateManager().hasPendingSequence(),
                            "Riptide block use must close the block prediction sequence");
                }
                player.input.sneaking = true;
                try {
                    client.world.setBlockState(target, Blocks.CRAFTING_TABLE.getDefaultState());
                    resetPackets();
                    check(client.interactionManager.interactBlock(player, hand, hit) == ActionResult.PASS,
                            "Sneaking must bypass the block interaction and allow local item use");
                    assertPackets(0, 0, 0, "Sneaking block fallback");
                } finally {
                    player.input.sneaking = false;
                }
            }
        }
        client.world.setBlockState(target, Blocks.AIR.getDefaultState());
        VillagerEntity villager = new VillagerEntity(EntityType.VILLAGER, client.world);
        villager.setId(-1_000_001);
        for (Hand hand : Hand.values()) {
            resetPackets();
            check(client.interactionManager.interactEntity(player, villager, hand).isAccepted(),
                    "Riptide simulation must preserve accepted entity interactions");
            assertPackets(0, 0, 0, "Accepted entity interaction");
        }
    }

    private static void verifyWaterAndFluidModels(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos center = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 3, 2))) {
            client.world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
        }
        player.setPosition(Vec3d.ofCenter(center));
        player.baseTick();
        check(player.isTouchingWater(), "Water probe must be in water");
        for (FluidMovementModel model : FluidMovementModel.values()) {
            BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(true);
            BetterMovingConfigs.MODEL.setOptionListValue(model);
            equipTrident(player, Hand.OFF_HAND, 2);
            startCharge(client, Hand.OFF_HAND);
            charge(player, 10);
            player.setOnGround(false);
            player.setVelocity(Vec3d.ZERO);
            client.interactionManager.stopUsingItem(player);
            assertClose(2.25, player.getVelocity().z, "Riptide works in water with either fluid model");
            assertPackets(0, 0, 0, "Water simulation");
        }
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
        verifyVanillaChargeInWater(client);
        for (BlockPos pos : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 3, 2))) {
            client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        player.baseTick();
        // Air rules also must not reintroduce the environmental restriction.
        BetterMovingConfigs.MODEL.setOptionListValue(FluidMovementModel.AIR);
        equipTrident(player, Hand.MAIN_HAND, 1);
        BlockPos target = player.getBlockPos().down();
        client.world.setBlockState(target, Blocks.STONE.getDefaultState());
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(target), Direction.UP, target, false);
        resetPackets();
        ActionResult blockResult = client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        check(blockResult == ActionResult.PASS, "Trident block fallback must remain PASS");
        check(blockUsePackets == 0, "Trident item fallback must not send a block-use packet");
        startCharge(client, Hand.MAIN_HAND);
        charge(player, 10);
        player.setOnGround(false);
        player.setVelocity(Vec3d.ZERO);
        client.interactionManager.stopUsingItem(player);
        assertClose(1.5, player.getVelocity().z, "Air rules must remain compatible");
        assertPackets(0, 0, 0, "Air-rule simulation");
        client.world.setBlockState(target, Blocks.AIR.getDefaultState());
        BetterMovingConfigs.IGNORE_FLUID_PHYSICS.setBooleanValue(false);
    }

    private static void verifyVanillaChargeInWater(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(true);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(0);
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(false);
        client.interactionManager.setGameMode(GameMode.SURVIVAL);
        for (int elapsed : new int[] {9, 10}) {
            equipTrident(player, Hand.MAIN_HAND, 3);
            resetPackets();
            check(client.interactionManager.interactItem(player, Hand.MAIN_HAND).isAccepted(),
                    "Vanilla water Riptide must still start with only custom charge enabled");
            check(!ClientRiptide.isCharging(player), "Disabled simulation must not own vanilla water use");
            charge(player, elapsed);
            player.setOnGround(false);
            player.setVelocity(Vec3d.ZERO);
            client.interactionManager.stopUsingItem(player);
            assertClose(elapsed == 10 ? 3.0 : 0.0, player.getVelocity().z,
                    "Parent-disabled water Riptide retains the vanilla threshold");
            assertPackets(1, 1, 0, "Parent-disabled water Riptide");
        }

        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);
        OtherClientPlayerEntity remote = new OtherClientPlayerEntity(client.world, player.getGameProfile());
        remote.setPosition(player.getPos());
        remote.baseTick();
        check(remote.isTouchingWater(), "Remote threshold fixture must be in water");
        remote.setYaw(0.0F);
        remote.setPitch(0.0F);
        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.addEnchantment(Enchantments.RIPTIDE, 3);
        remote.setStackInHand(Hand.MAIN_HAND, trident);
        for (int elapsed : new int[] {9, 10}) {
            check(trident.use(client.world, remote, Hand.MAIN_HAND).getResult().isAccepted(),
                    "Remote water Riptide must start vanilla use");
            ((RiptideStateProbe) remote).bettermovingTest$setItemUseTimeLeft(trident.getMaxUseTime() - elapsed);
            remote.setOnGround(false);
            remote.setVelocity(Vec3d.ZERO);
            remote.stopUsingItem();
            assertClose(elapsed == 10 ? 3.0 : 0.0, remote.getVelocity().z,
                    "Remote Riptide retains the vanilla threshold with both toggles enabled");
        }
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(false);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(10);
    }

    private static void verifyVanillaIsolation(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BetterMovingConfigs.CUSTOM_RIPTIDE_CHARGE_TIME.setBooleanValue(true);
        BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.setIntegerValue(0);
        OtherClientPlayerEntity remote = new OtherClientPlayerEntity(client.world, player.getGameProfile());
        ItemStack riptide = equipTrident(player, Hand.MAIN_HAND, 3);
        remote.setStackInHand(Hand.MAIN_HAND, riptide.copy());
        check(!ClientRiptide.shouldSimulate(remote, remote.getMainHandStack()),
                "Remote players must not simulate Riptide");
        check(remote.getMainHandStack().use(client.world, remote, Hand.MAIN_HAND).getResult()
                        == ActionResult.FAIL,
                "Remote Riptide in air must remain blocked");
        check(!remote.isUsingRiptide(), "Remote spin state must remain vanilla");

        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(false);
        resetPackets();
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND) == ActionResult.FAIL,
                "Disabled Riptide in air must retain vanilla failure");
        assertPackets(1, 0, 0, "Disabled toggle preserves vanilla use packet");
        BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.setBooleanValue(true);

        equipTrident(player, Hand.MAIN_HAND, 0);
        resetPackets();
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND).isAccepted(),
                "Unenchanted trident must still start vanilla use");
        charge(player, 10);
        client.interactionManager.stopUsingItem(player);
        assertPackets(1, 1, 0, "Unenchanted trident preserves vanilla use and release packets");

        client.interactionManager.setGameMode(GameMode.SPECTATOR);
        equipTrident(player, Hand.MAIN_HAND, 3);
        resetPackets();
        check(client.interactionManager.interactItem(player, Hand.MAIN_HAND) == ActionResult.PASS,
                "Spectators must not start Riptide");
        check(!player.isUsingItem(), "Spectator must not charge");
        assertPackets(0, 0, 0, "Spectator");
    }

    private static ItemStack equipTrident(ClientPlayerEntity player, Hand hand, int level) {
        player.clearActiveItem();
        ItemStack stack = new ItemStack(Items.TRIDENT);
        if (level > 0) {
            stack.addEnchantment(Enchantments.RIPTIDE, level);
        }
        player.setStackInHand(hand, stack);
        return stack;
    }

    private static void startCharge(MinecraftClient client, Hand hand) {
        resetPackets();
        check(client.interactionManager.interactItem(client.player, hand).isAccepted(),
                "Local Riptide use must be accepted");
        check(client.player.isUsingItem() && client.player.getActiveHand() == hand,
                "Riptide must use the correct hand and vanilla charge state");
        assertPackets(0, 0, 0, "Start charge");
    }

    private static void charge(ClientPlayerEntity player, int ticks) {
        // Advance real vanilla item usage without moving the test fixture.
        for (int i = 0; i < ticks; i++) {
            ((RiptideStateProbe) player).bettermovingTest$tickActiveItemStack();
        }
        check(player.getItemUseTime() == ticks, "Vanilla charge timer must advance");
    }

    private static void verifyMetadataDoesNotCancelCharge(ClientPlayerEntity player) {
        TrackedData<Byte> flags = RiptideStateProbe.bettermovingTest$getLivingFlags();
        // A server update to unrelated LivingEntity flags still has USING_ITEM=false.
        player.getDataTracker().set(flags, (byte) (player.getDataTracker().get(flags) ^ 4));
        player.getDataTracker().set(flags, (byte) 0);
        check(player.isUsingItem() && ClientRiptide.isCharging(player),
                "Server living-flag updates must not cancel local charging");
    }

    private static void resetPackets() {
        itemUsePackets = 0;
        releasePackets = 0;
        blockUsePackets = 0;
    }

    private static void assertPackets(int item, int release, int block, String context) {
        check(itemUsePackets == item && releasePackets == release && blockUsePackets == block,
                context + ": item=" + itemUsePackets + ", release=" + releasePackets
                        + ", block=" + blockUsePackets);
    }

    private static void assertClose(double expected, double actual, String context) {
        check(Math.abs(expected - actual) < 1.0E-5,
                context + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
