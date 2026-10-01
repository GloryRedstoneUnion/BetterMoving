package dev.bettermoving.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.bettermoving.gui.BetterMovingConfigScreen;

public final class BetterMovingModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterMovingConfigScreen::new;
    }
}
