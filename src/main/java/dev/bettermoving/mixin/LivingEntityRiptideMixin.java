package dev.bettermoving.mixin;

import dev.bettermoving.physics.ClientRiptide;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRiptideMixin {
    @Shadow
    protected int riptideTicks;

    @Inject(method = "isUsingRiptide", at = @At("RETURN"), cancellable = true)
    private void bettermoving$resolveLocalRiptideSpin(CallbackInfoReturnable<Boolean> cir) {
        if (this.riptideTicks > 0 && ClientRiptide.isSpinning((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
