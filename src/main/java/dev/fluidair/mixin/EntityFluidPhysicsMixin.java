package dev.fluidair.mixin;

import dev.fluidair.config.FluidAirConfigs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityFluidPhysicsMixin {
    @Unique
    private Vec3d fluidair$velocityBeforeFluidUpdate;

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Inject(method = "updateMovementInFluid", at = @At("HEAD"))
    private void fluidair$captureVelocityBeforeFluidUpdate(
            TagKey<Fluid> tag,
            double speed,
            CallbackInfoReturnable<Boolean> cir) {
        if (fluidair$shouldIgnorePhysics()
                && (tag == FluidTags.WATER || tag == FluidTags.LAVA)) {
            this.fluidair$velocityBeforeFluidUpdate = this.getVelocity();
        } else {
            this.fluidair$velocityBeforeFluidUpdate = null;
        }
    }

    @Inject(method = "updateMovementInFluid", at = @At("RETURN"))
    private void fluidair$restoreVelocityAfterFluidUpdate(
            TagKey<Fluid> tag,
            double speed,
            CallbackInfoReturnable<Boolean> cir) {
        if (this.fluidair$velocityBeforeFluidUpdate != null) {
            this.setVelocity(this.fluidair$velocityBeforeFluidUpdate);
            this.fluidair$velocityBeforeFluidUpdate = null;
        }
    }

    @Inject(
            method = {"onBubbleColumnSurfaceCollision", "onBubbleColumnCollision"},
            at = @At("HEAD"),
            cancellable = true)
    private void fluidair$ignoreBubbleColumnMovement(boolean drag, CallbackInfo ci) {
        if (fluidair$shouldIgnorePhysics()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean fluidair$shouldIgnorePhysics() {
        return FluidAirConfigs.ignoreFluidPhysics()
                && (Object) this instanceof ClientPlayerEntity;
    }
}
