package dev.fluidair.test;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.fluidair.gui.FluidAirConfigScreen;
import dev.fluidair.modmenu.FluidAirModMenuIntegration;
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
                .filter(FluidAirModMenuIntegration.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fluid Air Mod Menu entrypoint was not discovered"));

        Screen parent = client.currentScreen;
        Screen configScreen = api.getModConfigScreenFactory().create(parent);
        if (!(configScreen instanceof FluidAirConfigScreen)) {
            throw new AssertionError("Mod Menu did not create the Fluid Air MaLiLib config screen");
        }

        client.setScreen(configScreen);
        if (client.currentScreen != configScreen) {
            throw new AssertionError("Minecraft did not open the Fluid Air config screen");
        }
    }
}
