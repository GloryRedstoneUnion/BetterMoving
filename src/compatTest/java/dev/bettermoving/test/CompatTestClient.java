package dev.bettermoving.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatTestClient implements ClientModInitializer {
    private static final int STOP_AFTER_TICKS = 100;

    private int ticks;
    private boolean modMenuVerified;
    private final MovementCompatProbe movementProbe = Boolean.getBoolean("bettermoving.movementCompatTest")
                    || Boolean.getBoolean("bettermoving.platformCompatTest")
                    || Boolean.getBoolean("bettermoving.sprintCompatTest")
                    || Boolean.getBoolean("bettermoving.slipperinessCompatTest")
                    || Boolean.getBoolean("bettermoving.potionEffectsCompatTest")
            ? new MovementCompatProbe()
            : null;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.movementProbe != null) {
                this.movementProbe.tick(client);
                return;
            }
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
