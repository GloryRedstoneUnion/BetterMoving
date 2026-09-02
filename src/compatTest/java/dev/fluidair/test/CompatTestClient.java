package dev.fluidair.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class CompatTestClient implements ClientModInitializer {
    private static final int STOP_AFTER_TICKS = 100;

    private int ticks;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++this.ticks >= STOP_AFTER_TICKS) {
                client.scheduleStop();
            }
        });
    }
}
