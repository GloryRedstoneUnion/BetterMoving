package dev.fluidair.config;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

public final class FluidAirInitializationHandler implements IInitializationHandler {
    public static final FluidAirInitializationHandler INSTANCE =
            new FluidAirInitializationHandler();

    private FluidAirInitializationHandler() {
    }

    @Override
    public void registerModHandlers() {
        FluidAirConfigs.register();
        FluidAirKeybindProvider provider = new FluidAirKeybindProvider();
        InputEventHandler.getKeybindManager().registerKeybindProvider(provider);
        provider.installCallbacks();
    }
}
