package dev.bettermoving.mixin;

import dev.bettermoving.physics.ClientRiptide;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRiptideMixin {
    @Shadow
    protected int riptideTicks;

    @Shadow
    protected int itemUseTimeLeft;

    @Inject(method = "tickItemStackUsage", at = @At("RETURN"))
    private void bettermoving$limitCustomRiptideChargeTimer(ItemStack stack, CallbackInfo ci) {
        if (ClientRiptide.usesCustomChargeTime((LivingEntity) (Object) this)) {
            // Vanilla counts down past zero. Saturate elapsed ticks to prevent
            // getItemUseTime() and the release calculation from overflowing.
            int minimumTimeLeft = stack.getMaxUseTime() - Integer.MAX_VALUE;
            this.itemUseTimeLeft = Math.max(this.itemUseTimeLeft, minimumTimeLeft);
        }
    }

    @Inject(method = "isUsingRiptide", at = @At("RETURN"), cancellable = true)
    private void bettermoving$resolveLocalRiptideSpin(CallbackInfoReturnable<Boolean> cir) {
        if (this.riptideTicks > 0 && ClientRiptide.isSpinning((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
