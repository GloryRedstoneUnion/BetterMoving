package dev.bettermoving.test;

import dev.bettermoving.compat.ClientWorldPredictionProbe;
import dev.bettermoving.config.BetterMovingConfigs;
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
        try {
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
            if (player.getVelocity().z <= 0.0) {
                throw new AssertionError(
                        "The local Elytra firework did not apply vanilla acceleration: velocity="
                                + player.getVelocity());
            }
            for (int i = 0; i < 200 && !rocket.isRemoved(); ++i) {
                rocket.tick();
            }
            if (!rocket.isRemoved()) {
                throw new AssertionError("The local Elytra firework did not expire after its normal lifetime");
            }

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
            player.stopFallFlying();
            player.equipStack(EquipmentSlot.CHEST, previousChest);
            player.equipStack(EquipmentSlot.MAINHAND, previousMainHand);
            player.equipStack(EquipmentSlot.OFFHAND, previousOffHand);
            player.input.sneaking = previousSneaking;
            client.interactionManager.setGameMode(previousGameMode);
            player.setVelocity(Vec3d.ZERO);
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
