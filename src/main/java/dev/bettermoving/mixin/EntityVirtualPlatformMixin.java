package dev.bettermoving.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bettermoving.physics.VirtualPlatform;
import dev.bettermoving.physics.VoidProtectionPlatform;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityVirtualPlatformMixin {
    @ModifyExpressionValue(
            method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private List<VoxelShape> bettermoving$addVirtualPlatform(
            List<VoxelShape> collisions, Vec3d movement) {
        List<VoxelShape> result = VirtualPlatform.addMovementCollision(
                (Entity) (Object) this, movement, collisions);
        return VoidProtectionPlatform.addMovementCollision((Entity) (Object) this, movement, result);
    }
}
