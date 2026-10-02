package dev.bettermoving.compat;

import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface ClientWorldPredictionProbe {
    @Accessor("pendingUpdateManager")
    PendingUpdateManager bettermovingTest$getPendingUpdateManager();
}
