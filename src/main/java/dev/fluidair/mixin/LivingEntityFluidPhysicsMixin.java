package dev.fluidair.mixin;

import dev.fluidair.config.FluidAirConfigs;
import dev.fluidair.physics.FluidAirMovementPolicy;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidPhysicsMixin {
    @Redirect(
            method = {"tickMovement", "travel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isTouchingWater()Z"))
    private boolean fluidair$useAirTravelInWater(LivingEntity entity) {
        boolean ignoreFluidPhysics = fluidair$shouldIgnorePhysics(entity);
        return FluidAirMovementPolicy.useFluidJumpLogic(ignoreFluidPhysics)
                && entity.isTouchingWater();
    }

    @Redirect(
            method = {"tickMovement", "travel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z"))
    private boolean fluidair$useAirTravelInLava(LivingEntity entity) {
        boolean ignoreFluidPhysics = fluidair$shouldIgnorePhysics(entity);
        return FluidAirMovementPolicy.useFluidJumpLogic(ignoreFluidPhysics)
                && entity.isInLava();
    }

    @Unique
    private static boolean fluidair$shouldIgnorePhysics(LivingEntity entity) {
        return FluidAirConfigs.ignoreFluidPhysics()
                && entity instanceof ClientPlayerEntity;
    }
}
