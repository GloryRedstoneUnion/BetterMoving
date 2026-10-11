package dev.bettermoving.mixin;

import dev.bettermoving.physics.ElytraBoostState;
import dev.bettermoving.physics.ElytraHover;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraHoverMixin implements ElytraBoostState {
    @Shadow
    protected int riptideTicks;

    @Override
    @Unique
    public boolean bettermoving$hasRiptidePropulsion() {
        return this.riptideTicks > 0;
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void bettermoving$hoverBeforeUnpoweredTravel(Vec3d input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (ElytraHover.shouldHover(entity)) {
            entity.setVelocity(Vec3d.ZERO);
            entity.limitFallDistance();
            entity.updateLimbs(false);
            ci.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void bettermoving$stopAfterRiptideExpires(CallbackInfo ci) {
        // Vanilla decrements the spin timer and handles collision after travel.
        ElytraHover.stopIfUnpowered((LivingEntity) (Object) this);
    }

    @Inject(method = "tickRiptide", at = @At("RETURN"))
    private void bettermoving$stopAfterRiptideCollision(Box before, Box after, CallbackInfo ci) {
        ElytraHover.stopIfUnpowered((LivingEntity) (Object) this);
    }
}
