package dev.fluidair.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatTestClient implements ClientModInitializer {
    private static final int STOP_AFTER_TICKS = 100;

    private int ticks;
    private boolean modMenuVerified;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.modMenuVerified && FabricLoader.getInstance().isModLoaded("modmenu")) {
                ModMenuCompatProbe.openConfigScreen(client);
                this.modMenuVerified = true;
            }
            if (++this.ticks >= STOP_AFTER_TICKS) {
                client.scheduleStop();
            }
        });
    }
}
