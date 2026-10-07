package dev.bettermoving.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface RiptideStateProbe {
    @Invoker("tickActiveItemStack")
    void bettermovingTest$tickActiveItemStack();

    @Accessor("riptideTicks")
    void bettermovingTest$setRiptideTicks(int ticks);

    @Accessor("LIVING_FLAGS")
    static TrackedData<Byte> bettermovingTest$getLivingFlags() {
        throw new AssertionError("Living flags accessor was not applied");
    }

    @Invoker("tickRiptide")
    void bettermovingTest$tickRiptide(Box before, Box after);
}
