package dev.bettermoving.mixin;

import dev.bettermoving.entity.ClientFireworkRocket;
import dev.bettermoving.entity.ClientFireworkRocketManager;
import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.physics.ElytraFireworkPolicy;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.OptionalInt;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements ClientFireworkRocket {
    @Shadow @Final
    private static TrackedData<OptionalInt> SHOOTER_ENTITY_ID;

    @Shadow
    private int life;

    @Shadow
    private int lifeTime;

    @Shadow
    private LivingEntity shooter;

    @Unique
    private boolean bettermoving$localSimulation;

    @Unique
    private boolean bettermoving$customLifetimeApplied;

    @Inject(method = {
            "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V",
            "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V"
    }, at = @At("RETURN"))
    private void bettermoving$trackAttachedRocket(CallbackInfo ci) {
        ClientFireworkRocketManager.track((FireworkRocketEntity) (Object) this);
    }

    @Override
    @Unique
    public boolean bettermoving$isBoosting(ClientPlayerEntity player) {
        FireworkRocketEntity rocket = (FireworkRocketEntity) (Object) this;
        OptionalInt shooterId = rocket.getDataTracker().get(SHOOTER_ENTITY_ID);
        return !rocket.isRemoved()
                && rocket.getWorld() == player.getWorld()
                && (!this.bettermoving$localSimulation || this.life <= this.lifeTime)
                && (this.shooter == player
                        || (shooterId.isPresent() && shooterId.getAsInt() == player.getId()));
    }

    @Override
    @Unique
    public void bettermoving$markLocalSimulation() {
        this.bettermoving$localSimulation = true;
    }

    @Override
    @Unique
    public void bettermoving$discardIfLocalSimulation() {
        if (this.bettermoving$localSimulation) {
            ((FireworkRocketEntity) (Object) this).discard();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bettermoving$applyCustomLifetime(CallbackInfo ci) {
        if (this.bettermoving$customLifetimeApplied) {
            return;
        }
        this.bettermoving$customLifetimeApplied = true;
        MinecraftClient client = MinecraftClient.getInstance();
        FireworkRocketEntity rocket = (FireworkRocketEntity) (Object) this;
        if (!rocket.getWorld().isClient
                || !BetterMovingConfigs.CUSTOM_ELYTRA_FIREWORK_LIFETIME.getBooleanValue()
                || this.shooter == null
                || this.shooter != client.player
                || !this.shooter.isFallFlying()) {
            return;
        }
        NbtCompound fireworks = rocket.getStack().getSubNbt("Fireworks");
        if (fireworks == null) {
            return;
        }
        int flight = fireworks.contains("Flight", NbtCompound.BYTE_TYPE)
                ? fireworks.getByte("Flight")
                : 0;
        this.lifeTime = ElytraFireworkPolicy.customLifetime(
                true,
                flight,
                this.lifeTime,
                BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_1.getIntegerValue(),
                BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_2.getIntegerValue(),
                BetterMovingConfigs.ELYTRA_FIREWORK_LIFETIME_FLIGHT_3.getIntegerValue());
    }

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
                    ordinal = 0),
            index = 0)
    private Vec3d bettermoving$applyTargetSpeed(Vec3d vanillaVelocity) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localFallFlyingPlayer = this.shooter != null
                && this.shooter == client.player
                && this.shooter.isFallFlying();
        return ElytraFireworkPolicy.applyTargetSpeed(
                vanillaVelocity,
                localFallFlyingPlayer ? this.shooter.getVelocity() : Vec3d.ZERO,
                localFallFlyingPlayer ? this.shooter.getRotationVector() : Vec3d.ZERO,
                BetterMovingConfigs.SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED.getDoubleValue(),
                BetterMovingConfigs.SIMULATE_ELYTRA_FIREWORK_SPEED.getBooleanValue()
                        && localFallFlyingPlayer);
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
