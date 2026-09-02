package dev.fluidair.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TridentItem.class, priority = 2100)
public abstract class TridentItemMovementMixin {
    @WrapOperation(
            method = {"use", "onStoppedUsing"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"))
    private boolean fluidair$resolveRiptideEnvironment(
            PlayerEntity player,
            Operation<Boolean> original) {
        BlockPos pos = player.getBlockPos();
        boolean rainingAtPlayer = player.getWorld().hasRain(pos)
                || player.getWorld().hasRain(BlockPos.ofFloored(
                        pos.getX(),
                        player.getBoundingBox().maxY,
                        pos.getZ()));
        return FluidAirMovementPolicy.resolveRiptideEnvironment(
                FluidAirConfigs.ignoreFluidPhysics(),
                FluidAirConfigs.movementModel(),
                player instanceof ClientPlayerEntity,
                original.call(player),
                rainingAtPlayer,
                player.isInLava());
    }
}
