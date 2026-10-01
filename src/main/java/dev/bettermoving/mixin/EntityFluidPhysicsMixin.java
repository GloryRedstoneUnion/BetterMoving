package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.BetterMovingMovementPolicy;
import dev.bettermoving.physics.FluidMovementContext;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 2100)
public abstract class EntityFluidPhysicsMixin {
    @Shadow
    protected boolean firstUpdate;

    @Shadow
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;

    @Shadow
    public abstract Vec3d getVelocity();

    @ModifyReturnValue(method = "isTouchingWater", at = @At("RETURN"))
    private boolean bettermoving$resolveTouchingWaterState(boolean detectedWaterState) {
        Entity entity = (Entity) (Object) this;
        return FluidMovementContext.resolveWaterState(
                entity,
                detectedWaterState,
                bettermoving$isTouchingLavaRaw());
    }

    @ModifyReturnValue(method = "isSubmergedInWater", at = @At("RETURN"))
    private boolean bettermoving$resolveSubmergedInWaterState(boolean detectedWaterState) {
        Entity entity = (Entity) (Object) this;
        return FluidMovementContext.resolveSubmergedWaterState(
                entity,
                detectedWaterState);
    }

    @ModifyReturnValue(method = "isInLava", at = @At("RETURN"))
    private boolean bettermoving$resolveLavaState(boolean detectedLavaState) {
        return FluidMovementContext.resolveLavaState(
                (Entity) (Object) this,
                detectedLavaState);
    }

    @ModifyReturnValue(method = "getFluidHeight", at = @At("RETURN"))
    private double bettermoving$resolveFluidHeight(double detectedHeight, TagKey<Fluid> fluid) {
        return FluidMovementContext.resolveFluidHeight(
                (Entity) (Object) this,
                fluid,
                detectedHeight,
                this.fluidHeight.getDouble(FluidTags.WATER),
                this.fluidHeight.getDouble(FluidTags.LAVA));
    }

    @ModifyArg(
            method = "updateMovementInFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"),
            index = 0)
    private Vec3d bettermoving$ignoreFluidCurrent(Vec3d velocity) {
        return bettermoving$shouldApplyWaterMovement() ? velocity : this.getVelocity();
    }

    @ModifyArg(
            method = "updateWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z"),
            index = 1)
    private double bettermoving$useWaterCurrentSpeedInLava(double vanillaSpeed) {
        return BetterMovingMovementPolicy.resolveLavaCurrentSpeed(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                bettermoving$isLocalPlayer(),
                vanillaSpeed);
    }

    @ModifyExpressionValue(
            method = "getVelocityMultiplier",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean bettermoving$resolveFluidVelocityMultiplier(boolean fluidBlock) {
        return bettermoving$resolveTouchingWater(fluidBlock);
    }

    @WrapOperation(
            method = "updateSwimming",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/fluid/FluidState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean bettermoving$resolveSwimmingFluidState(
            FluidState fluidState,
            TagKey<Fluid> fluid,
            Operation<Boolean> original) {
        return FluidMovementContext.resolveWaterState(
                (Entity) (Object) this,
                original.call(fluidState, fluid),
                fluidState.isIn(FluidTags.LAVA));
    }

    @WrapOperation(
            method = "checkWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;onLanding()V"))
    private void bettermoving$preserveFallDistanceInWater(
            Entity entity,
            Operation<Void> original) {
        if (bettermoving$shouldApplyWaterMovement()) {
            original.call(entity);
        }
    }

    @ModifyExpressionValue(
            method = "baseTick",
            at = @At(value = "CONSTANT", args = "floatValue=0.5"))
    private float bettermoving$resolveFallDistanceInLava(float vanillaMultiplier) {
        if (BetterMovingMovementPolicy.shouldResetLavaFallDistanceAsWater(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                bettermoving$isLocalPlayer())) {
            ((Entity) (Object) this).onLanding();
        }
        return BetterMovingMovementPolicy.resolveLavaFallDistanceMultiplier(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                bettermoving$isLocalPlayer(),
                vanillaMultiplier);
    }

    @Inject(
            method = {"onBubbleColumnSurfaceCollision", "onBubbleColumnCollision"},
            at = @At("HEAD"),
            cancellable = true)
    private void bettermoving$resolveBubbleColumnMovement(boolean drag, CallbackInfo ci) {
        if (!BetterMovingMovementPolicy.shouldApplyBubbleColumnMovement(
                BetterMovingConfigs.ignoreFluidPhysics(),
                bettermoving$isLocalPlayer())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean bettermoving$shouldApplyWaterMovement() {
        return BetterMovingMovementPolicy.shouldApplyWaterMovement(
                BetterMovingConfigs.ignoreFluidPhysics(),
                BetterMovingConfigs.movementModel(),
                bettermoving$isLocalPlayer());
    }

    @Unique
    private boolean bettermoving$resolveTouchingWater(boolean detectedWaterState) {
        return FluidMovementContext.resolveWaterState(
                (Entity) (Object) this,
                detectedWaterState,
                bettermoving$isTouchingLavaRaw());
    }

    @Unique
    private boolean bettermoving$isTouchingLavaRaw() {
        return !this.firstUpdate && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
    }

    @Unique
    private boolean bettermoving$isLocalPlayer() {
        return (Object) this instanceof ClientPlayerEntity;
    }
}
