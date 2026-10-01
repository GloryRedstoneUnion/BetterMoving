package dev.bettermoving;

import dev.bettermoving.config.BetterMovingInitializationHandler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterMovingClient implements ClientModInitializer {
    public static final String MOD_ID = "bettermoving";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance()
                .registerInitializationHandler(BetterMovingInitializationHandler.INSTANCE);
        LOGGER.info("BetterMoving initialized");
    }
}
