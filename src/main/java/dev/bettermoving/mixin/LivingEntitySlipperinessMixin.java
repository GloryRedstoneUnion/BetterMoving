package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.SlipperinessPolicy;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySlipperinessMixin {
    @WrapOperation(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getSlipperiness()F"))
    private float bettermoving$resolveSlipperiness(
            Block block,
            Operation<Float> original) {
        return SlipperinessPolicy.resolve(
                BetterMovingConfigs.IGNORE_SLIPPERY_BLOCKS.getBooleanValue(),
                (Object) this == MinecraftClient.getInstance().player,
                original.call(block));
    }
}
