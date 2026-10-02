package dev.bettermoving;

import dev.bettermoving.config.BetterMovingInitializationHandler;
import dev.bettermoving.physics.LevitationElytraFlight;
import dev.bettermoving.physics.VirtualPlatform;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterMovingClient implements ClientModInitializer {
    public static final String MOD_ID = "bettermoving";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(VirtualPlatform::tick);
        ClientTickEvents.START_CLIENT_TICK.register(LevitationElytraFlight::tick);
        InitializationHandler.getInstance()
                .registerInitializationHandler(BetterMovingInitializationHandler.INSTANCE);
        LOGGER.info("BetterMoving initialized");
    }
}
