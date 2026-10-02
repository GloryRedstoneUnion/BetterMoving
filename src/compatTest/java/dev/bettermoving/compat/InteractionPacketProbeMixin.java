package dev.bettermoving.compat;

import dev.bettermoving.test.ElytraFireworkCompatProbe;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class InteractionPacketProbeMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void bettermovingTest$observeInteractionPacket(Packet<?> packet, CallbackInfo ci) {
        ElytraFireworkCompatProbe.observePacket(packet);
    }
}
