package dev.bettermoving.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityFlagsProbe {
    @Accessor("FLAGS")
    static TrackedData<Byte> bettermovingTest$getFlags() {
        throw new AssertionError("Entity flags accessor was not applied");
    }
}
