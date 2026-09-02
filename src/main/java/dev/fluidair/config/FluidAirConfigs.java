package dev.fluidair.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fluidair.FluidAirClient;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import java.io.File;
import java.util.List;

public final class FluidAirConfigs implements IConfigHandler {
    public static final ConfigBooleanHotkeyed IGNORE_FLUID_PHYSICS =
            new ConfigBooleanHotkeyed(
                    "ignoreFluidPhysics",
                    false,
                    "",
                    comment("ignoreFluidPhysics"),
                    prettyName("ignoreFluidPhysics"));

    public static final ConfigHotkey OPEN_CONFIG_GUI =
            new ConfigHotkey(
                    "openConfigGui",
                    "L,C",
                    comment("openConfigGui"),
                    prettyName("openConfigGui"));

    public static final List<IConfigBase> OPTIONS = List.of(IGNORE_FLUID_PHYSICS);
    public static final List<ConfigHotkey> STANDALONE_HOTKEYS = List.of(OPEN_CONFIG_GUI);
    public static final List<IHotkey> ALL_HOTKEYS = List.of(
            IGNORE_FLUID_PHYSICS,
            OPEN_CONFIG_GUI);
    public static final List<IConfigBase> GUI_OPTIONS = List.of(
            IGNORE_FLUID_PHYSICS,
            OPEN_CONFIG_GUI);

    public static final FluidAirConfigs INSTANCE = new FluidAirConfigs();

    private static final File CONFIG_FILE =
            new File(FileUtils.getConfigDirectory(), "fluidair.json");

    private FluidAirConfigs() {
    }

    public static void register() {
        ConfigManager.getInstance().registerConfigHandler(FluidAirClient.MOD_ID, INSTANCE);
    }

    public static boolean ignoreFluidPhysics() {
        return IGNORE_FLUID_PHYSICS.getBooleanValue();
    }

    @Override
    public void load() {
        JsonElement root = JsonUtils.parseJsonFile(CONFIG_FILE);
        if (root != null && root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            ConfigUtils.readConfigBase(object, "options", OPTIONS);
            ConfigUtils.readHotkeys(object, "hotkeys", STANDALONE_HOTKEYS);
        }
    }

    @Override
    public void save() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "options", OPTIONS);
        ConfigUtils.writeHotkeys(root, "hotkeys", STANDALONE_HOTKEYS);
        if (!JsonUtils.writeJsonToFile(root, CONFIG_FILE)) {
            FluidAirClient.LOGGER.error("Failed to write config file {}", CONFIG_FILE);
        }
    }

    private static String comment(String name) {
        return "fluidair.config.comment." + name;
    }

    private static String prettyName(String name) {
        return "fluidair.config.name." + name;
    }
}
