package dev.bettermoving.mixin;

import dev.bettermoving.entity.ClientFireworkRocket;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements ClientFireworkRocket {
    @Shadow
    private int life;

    @Shadow
    private int lifeTime;

    @Unique
    private boolean bettermoving$localSimulation;

    @Override
    @Unique
    public void bettermoving$markLocalSimulation() {
        this.bettermoving$localSimulation = true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bettermoving$discardLocalSimulation(CallbackInfo ci) {
        if (this.bettermoving$localSimulation
                && ((FireworkRocketEntity) (Object) this).getWorld().isClient
                && this.life > this.lifeTime) {
            ((FireworkRocketEntity) (Object) this).discard();
        }
    }
}
