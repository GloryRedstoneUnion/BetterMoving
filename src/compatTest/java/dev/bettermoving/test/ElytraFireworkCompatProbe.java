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
import net.minecraft.util.math.Vec3d;

public final class ElytraFireworkCompatProbe {
    private ElytraFireworkCompatProbe() {
    }

    public static void verify(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ItemStack previousChest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack previousMainHand = player.getMainHandStack();
        boolean previousToggle = BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.getBooleanValue();
        try {
            player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FIREWORK_ROCKET, 3));
            player.setOnGround(false);
            player.setVelocity(Vec3d.ZERO);
            player.setYaw(0.0F);
            player.setPitch(0.0F);
            player.startFallFlying();
            BetterMovingConfigs.INFINITE_ELYTRA_FIREWORKS.setBooleanValue(true);

            ActionResult result = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            if (!result.isAccepted() || player.getMainHandStack().getCount() != 3) {
                throw new AssertionError(
                        "Infinite Elytra fireworks did not preserve the local rocket stack: result="
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
            player.stopFallFlying();
            player.equipStack(EquipmentSlot.CHEST, previousChest);
            player.equipStack(EquipmentSlot.MAINHAND, previousMainHand);
            player.setVelocity(Vec3d.ZERO);
        }
    }
}
