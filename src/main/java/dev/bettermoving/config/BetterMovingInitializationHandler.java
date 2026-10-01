package dev.bettermoving.config;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;

public final class BetterMovingInitializationHandler implements IInitializationHandler {
    public static final BetterMovingInitializationHandler INSTANCE =
            new BetterMovingInitializationHandler();

    private BetterMovingInitializationHandler() {
    }

    @Override
    public void registerModHandlers() {
        BetterMovingConfigs.register();
        BetterMovingKeybindProvider provider = new BetterMovingKeybindProvider();
        InputEventHandler.getKeybindManager().registerKeybindProvider(provider);
        provider.installCallbacks();
    }
}
