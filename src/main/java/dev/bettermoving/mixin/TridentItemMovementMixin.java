package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.BetterMovingMovementPolicy;
import dev.bettermoving.physics.ClientRiptide;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = TridentItem.class, priority = 2100)
public abstract class TridentItemMovementMixin {
    @ModifyConstant(method = "onStoppedUsing", constant = @Constant(intValue = 10))
    private int bettermoving$resolveRiptideChargeTime(
            int original, ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        return ClientRiptide.usesCustomChargeTime(user) && user.getActiveItem() == stack
                ? BetterMovingConfigs.RIPTIDE_CHARGE_TIME_TICKS.getIntegerValue()
                : original;
    }

    @WrapOperation(
            method = {"use", "onStoppedUsing"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean bettermoving$resolveRiptideEnvironment(
            PlayerEntity player,
            Operation<Boolean> original) {
        if (BetterMovingConfigs.SIMULATE_RIPTIDE_ANYWHERE.getBooleanValue()
                && player == MinecraftClient.getInstance().player) {
            return true;
        }
        BlockPos pos = player.getBlockPos();
        boolean rainingAtPlayer = player.getWorld().hasRain(pos)
                || player.getWorld().hasRain(BlockPos.ofFloored(
                        pos.getX(),
                        player.getBoundingBox().maxY,
                        pos.getZ()));
        return BetterMovingMovementPolicy.resolveRiptideEnvironment(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                player instanceof ClientPlayerEntity,
                original.call(player),
                rainingAtPlayer,
                player.isInLava());
    }

    @WrapOperation(
            method = "onStoppedUsing",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
    private void bettermoving$playLocalRiptideSound(
            World world, PlayerEntity except, Entity entity, SoundEvent sound,
            SoundCategory category, float volume, float pitch, Operation<Void> original) {
        // ClientWorld plays this sound only when 'except' is its local player.
        if (entity instanceof PlayerEntity player && ClientRiptide.isCharging(player)) {
            except = player;
        }
        original.call(world, except, entity, sound, category, volume, pitch);
    }
}
