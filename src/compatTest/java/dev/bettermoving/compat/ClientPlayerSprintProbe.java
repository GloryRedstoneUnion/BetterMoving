package dev.bettermoving.compat;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerSprintProbe {
    @Invoker("canSprint")
    boolean bettermovingTest$canSprint();

    @Invoker("canStartSprinting")
    boolean bettermovingTest$canStartSprinting();
}
