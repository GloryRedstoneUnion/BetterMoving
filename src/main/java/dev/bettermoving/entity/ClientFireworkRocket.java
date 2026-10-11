package dev.bettermoving.entity;

import net.minecraft.client.network.ClientPlayerEntity;

public interface ClientFireworkRocket {
    void bettermoving$markLocalSimulation();

    void bettermoving$discardIfLocalSimulation();

    boolean bettermoving$isBoosting(ClientPlayerEntity player);
}
