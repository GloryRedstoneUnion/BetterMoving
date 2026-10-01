package dev.bettermoving.test;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.bettermoving.gui.BetterMovingConfigScreen;
import dev.bettermoving.modmenu.BetterMovingModMenuIntegration;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class ModMenuCompatProbe {
    private ModMenuCompatProbe() {
    }

    public static void openConfigScreen(MinecraftClient client) {
        ModMenuApi api = FabricLoader.getInstance()
                .getEntrypoints("modmenu", ModMenuApi.class)
                .stream()
                .filter(BetterMovingModMenuIntegration.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("BetterMoving Mod Menu entrypoint was not discovered"));

        Screen parent = client.currentScreen;
        Screen configScreen = api.getModConfigScreenFactory().create(parent);
        if (!(configScreen instanceof BetterMovingConfigScreen)) {
            throw new AssertionError("Mod Menu did not create the BetterMoving MaLiLib config screen");
        }

        client.setScreen(configScreen);
        if (client.currentScreen != configScreen) {
            throw new AssertionError("Minecraft did not open the BetterMoving config screen");
        }
    }
}
