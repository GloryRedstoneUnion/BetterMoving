package dev.bettermoving.test;

import dev.bettermoving.config.BetterMovingConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class ElytraFireworkCompatProbe {
    private ElytraFireworkCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ItemStack previousChest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack previousMainHand = player.getMainHandStack();
        boolean previousToggle = BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue();
        boolean previousBlockUseToggle = BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.getBooleanValue();
        boolean previousBlockNonElytraUseToggle =
                BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.getBooleanValue();
        try {
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
            player.setOnGround(false);
            player.setVelocity(Vec3d.ZERO);
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);
            BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.setBooleanValue(true);

            BlockPos target = player.getBlockPos().down();
            client.world.setBlockState(target, net.minecraft.block.Blocks.STONE.getDefaultState());
            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(false);
            player.stopFallFlying();
            ActionResult result = client.interactionManager.interactBlock(
                    player,
                    Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(target), Direction.NORTH, target, false));
            if (result != ActionResult.FAIL || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Non-Elytra firework block use was not fully blocked on the ground: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }

            player.startFallFlying();
            result = client.interactionManager.interactBlock(
                    player,
                    Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(target), Direction.NORTH, target, false));
            if (result != ActionResult.FAIL || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Firework block use was not blocked while gliding without the Elytra route: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }

            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(true);
            result = client.interactionManager.interactBlock(
                    player,
                    Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(target), Direction.NORTH, target, false));
            if (!result.isAccepted() || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Elytra firework block use did not preserve the local rocket stack: result="
                                + result
                                + ", count="
                                + player.getMainHandStack().getCount());
            }

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
            System.out.println("[bettermoving] Elytra firework compatibility checks passed");
        } finally {
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(previousToggle);
            BetterMovingConfigs.ELYTRA_FIREWORK_BLOCK_USE.setBooleanValue(previousBlockUseToggle);
            BetterMovingConfigs.BLOCK_NON_ELYTRA_FIREWORK_USE.setBooleanValue(previousBlockNonElytraUseToggle);
            player.stopFallFlying();
            player.equipStack(EquipmentSlot.CHEST, previousChest);
            player.equipStack(EquipmentSlot.MAINHAND, previousMainHand);
            player.setVelocity(Vec3d.ZERO);
        }
    }
}
