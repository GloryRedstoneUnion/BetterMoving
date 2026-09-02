package dev.fluidair.gui;

import dev.fluidair.FluidAirClient;
import dev.fluidair.config.FluidAirConfigs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;

public final class FluidAirConfigScreen extends GuiConfigsBase {
    public FluidAirConfigScreen(Screen parent) {
        super(10, 50, FluidAirClient.MOD_ID, parent, "fluidair.gui.title");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(FluidAirConfigs.GUI_OPTIONS);
    }

    @Override
    protected int getConfigWidth() {
        return 200;
    }
}
