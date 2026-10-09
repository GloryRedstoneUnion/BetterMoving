package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.physics.VirtualPlatform;
import dev.bettermoving.physics.VoidProtectionPlatform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityVirtualPlatformMixin {
    @WrapOperation(
            method = {"adjustMovementForSneaking", "method_30263"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;isSpaceEmpty(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Z"))
    private boolean bettermoving$includeVirtualPlatformInLedgeCheck(
            World world, Entity entity, Box box, Operation<Boolean> original) {
        return original.call(world, entity, box)
                && !VirtualPlatform.intersectsMovementSupport(entity, box)
                && !VoidProtectionPlatform.intersectsMovementSupport(entity, box);
    }

    @WrapOperation(
            method = "getBlockBreakingSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;isOnGround()Z"))
    private boolean bettermoving$ignoreVirtualPlatformForMining(
            PlayerEntity player, Operation<Boolean> original) {
        return original.call(player) && !VirtualPlatform.isVirtualOnlyMiningSupport(player);
    }
}
