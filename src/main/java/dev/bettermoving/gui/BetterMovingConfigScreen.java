package dev.bettermoving.gui;

import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.config.BetterMovingConfigs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;

public final class BetterMovingConfigScreen extends GuiConfigsBase {
    public BetterMovingConfigScreen(Screen parent) {
        super(10, 50, BetterMovingClient.MOD_ID, parent, "bettermoving.gui.title");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(BetterMovingConfigs.GUI_OPTIONS);
    }

    @Override
    protected int getConfigWidth() {
        return 200;
    }
}
