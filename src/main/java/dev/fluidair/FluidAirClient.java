package dev.fluidair;

import dev.fluidair.config.FluidAirInitializationHandler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FluidAirClient implements ClientModInitializer {
    public static final String MOD_ID = "fluidair";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance()
                .registerInitializationHandler(FluidAirInitializationHandler.INSTANCE);
        LOGGER.info("Fluid Air initialized");
    }
}
