package dev.fluidair.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.fluidair.gui.FluidAirConfigScreen;

public final class FluidAirModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FluidAirConfigScreen::new;
    }
}
