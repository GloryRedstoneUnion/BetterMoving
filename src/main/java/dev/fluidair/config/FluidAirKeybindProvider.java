package dev.fluidair.config;

import dev.fluidair.gui.FluidAirConfigScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.util.GuiUtils;

public final class FluidAirKeybindProvider implements IKeybindProvider {
    @Override
    public void addKeysToMap(IKeybindManager manager) {
        FluidAirConfigs.ALL_HOTKEYS.forEach(
                hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                "fluidair.hotkeys.category",
                "Fluid Air",
                FluidAirConfigs.ALL_HOTKEYS);
    }

    public void installCallbacks() {
        FluidAirConfigs.OPEN_CONFIG_GUI.getKeybind().setCallback((action, keybind) -> {
            GuiBase.openGui(new FluidAirConfigScreen(GuiUtils.getCurrentScreen()));
            return true;
        });
    }
}
