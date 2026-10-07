package dev.bettermoving.config;

import dev.bettermoving.gui.BetterMovingConfigScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.GuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class BetterMovingKeybindProvider implements IKeybindProvider {
    @Override
    public void addKeysToMap(IKeybindManager manager) {
        BetterMovingConfigs.ALL_HOTKEYS.forEach(
                hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                "bettermoving.hotkeys.category",
                "BetterMoving",
                BetterMovingConfigs.ALL_HOTKEYS);
    }

    public void installCallbacks() {
        BetterMovingConfigs.RESET_MOMENTUM.getKeybind().setCallback((action, keybind) -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (action != KeyAction.PRESS || player == null) {
                return false;
            }
            player.setVelocity(Vec3d.ZERO);
            return true;
        });
        BetterMovingConfigs.OPEN_CONFIG_GUI.getKeybind().setCallback((action, keybind) -> {
            GuiBase.openGui(new BetterMovingConfigScreen(GuiUtils.getCurrentScreen()));
            return true;
        });
    }
}
