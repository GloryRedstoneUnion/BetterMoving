package dev.bettermoving.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.physics.VirtualPlatform;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import java.io.File;
import java.util.List;

public final class BetterMovingConfigs implements IConfigHandler {
    public static final ConfigBooleanHotkeyed IGNORE_FLUID_PHYSICS =
            new ConfigBooleanHotkeyed(
                    "ignoreFluidPhysics",
                    false,
                    "",
                    comment("ignoreFluidPhysics"),
                    prettyName("ignoreFluidPhysics"));

    public static final ConfigOptionList MODEL =
            new ConfigOptionList(
                    "model",
                    FluidMovementModel.AIR,
                    comment("model"),
                    prettyName("model"));

    public static final ConfigBooleanHotkeyed VIRTUAL_PLATFORM =
            new ConfigBooleanHotkeyed(
                    "virtualPlatform",
                    false,
                    "",
                    comment("virtualPlatform"),
                    prettyName("virtualPlatform"));

    public static final ConfigBooleanHotkeyed IGNORE_SPRINT_HUNGER =
            new ConfigBooleanHotkeyed(
                    "ignoreSprintHunger",
                    false,
                    "",
                    comment("ignoreSprintHunger"),
                    prettyName("ignoreSprintHunger"));

    public static final ConfigBooleanHotkeyed IGNORE_SLIPPERY_BLOCKS =
            new ConfigBooleanHotkeyed(
                    "ignoreSlipperyBlocks",
                    false,
                    "",
                    comment("ignoreSlipperyBlocks"),
                    prettyName("ignoreSlipperyBlocks"));

    public static final ConfigBooleanHotkeyed SIMULATE_POTION_EFFECTS =
            new ConfigBooleanHotkeyed(
                    "simulatePotionEffects",
                    false,
                    "",
                    comment("simulatePotionEffects"),
                    prettyName("simulatePotionEffects"));

    public static final ConfigBooleanHotkeyed OVERRIDE_POTION_EFFECTS =
            new ConfigBooleanHotkeyed(
                    "overridePotionEffects",
                    false,
                    "",
                    comment("overridePotionEffects"),
                    prettyName("overridePotionEffects"));

    public static final ConfigBooleanHotkeyed IGNORE_LEVITATION_AND_SLOWNESS =
            new ConfigBooleanHotkeyed(
                    "ignoreLevitationAndSlowness",
                    false,
                    "",
                    comment("ignoreLevitationAndSlowness"),
                    prettyName("ignoreLevitationAndSlowness"));

    public static final ConfigInteger SIMULATED_SPEED_POTION_LEVEL =
            new ConfigInteger(
                    "simulatedSpeedPotionLevel",
                    0,
                    0,
                    255,
                    false,
                    comment("simulatedSpeedPotionLevel"));

    public static final ConfigInteger SIMULATED_JUMP_BOOST_LEVEL =
            new ConfigInteger(
                    "simulatedJumpBoostLevel",
                    0,
                    0,
                    255,
                    false,
                    comment("simulatedJumpBoostLevel"));

    public static final ConfigInteger SIMULATED_DOLPHINS_GRACE_LEVEL =
            new ConfigInteger(
                    "simulatedDolphinsGraceLevel",
                    0,
                    0,
                    255,
                    false,
                    comment("simulatedDolphinsGraceLevel"));

    public static final ConfigHotkey OPEN_CONFIG_GUI =
            new ConfigHotkey(
                    "openConfigGui",
                    "L,C",
                    comment("openConfigGui"),
                    prettyName("openConfigGui"));

    public static final List<IConfigBase> OPTIONS = List.of(
            IGNORE_FLUID_PHYSICS,
            MODEL,
            VIRTUAL_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            SIMULATED_SPEED_POTION_LEVEL,
            SIMULATED_JUMP_BOOST_LEVEL,
            SIMULATED_DOLPHINS_GRACE_LEVEL);
    public static final List<ConfigHotkey> STANDALONE_HOTKEYS = List.of(OPEN_CONFIG_GUI);
    public static final List<IHotkey> ALL_HOTKEYS = List.of(
            IGNORE_FLUID_PHYSICS,
            VIRTUAL_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            OPEN_CONFIG_GUI);
    public static final List<IConfigBase> GUI_OPTIONS = List.of(
            IGNORE_FLUID_PHYSICS,
            MODEL,
            VIRTUAL_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            SIMULATED_SPEED_POTION_LEVEL,
            SIMULATED_JUMP_BOOST_LEVEL,
            SIMULATED_DOLPHINS_GRACE_LEVEL,
            OPEN_CONFIG_GUI);

    public static final BetterMovingConfigs INSTANCE = new BetterMovingConfigs();

    private static final File CONFIG_FILE =
            new File(FileUtils.getConfigDirectory(), "bettermoving.json");
    private static final File LEGACY_CONFIG_FILE =
            new File(FileUtils.getConfigDirectory(), "fluidair.json");

    private BetterMovingConfigs() {
    }

    public static void register() {
        VIRTUAL_PLATFORM.setValueChangeCallback(config -> VirtualPlatform.onOptionChanged());
        ConfigManager.getInstance().registerConfigHandler(BetterMovingClient.MOD_ID, INSTANCE);
    }

    public static boolean ignoreFluidPhysics() {
        return IGNORE_FLUID_PHYSICS.getBooleanValue();
    }

    public static FluidMovementModel movementModel() {
        return (FluidMovementModel) MODEL.getOptionListValue();
    }

    public static boolean simulatePotionEffects() {
        return SIMULATE_POTION_EFFECTS.getBooleanValue();
    }

    public static boolean overridePotionEffects() {
        return OVERRIDE_POTION_EFFECTS.getBooleanValue();
    }

    public static boolean ignoreLevitationAndSlowness() {
        return IGNORE_LEVITATION_AND_SLOWNESS.getBooleanValue();
    }

    @Override
    public void load() {
        File configFile = CONFIG_FILE.exists() ? CONFIG_FILE : LEGACY_CONFIG_FILE;
        JsonElement root = JsonUtils.parseJsonFile(configFile);
        if (root != null && root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            ConfigUtils.readConfigBase(object, "options", OPTIONS);
            ConfigUtils.readHotkeys(object, "hotkeys", STANDALONE_HOTKEYS);
            if (configFile == LEGACY_CONFIG_FILE) {
                save();
            }
        }
    }

    @Override
    public void save() {
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "options", OPTIONS);
        ConfigUtils.writeHotkeys(root, "hotkeys", STANDALONE_HOTKEYS);
        if (!JsonUtils.writeJsonToFile(root, CONFIG_FILE)) {
            BetterMovingClient.LOGGER.error("Failed to write config file {}", CONFIG_FILE);
        }
    }

    private static String comment(String name) {
        return "bettermoving.config.comment." + name;
    }

    private static String prettyName(String name) {
        return "bettermoving.config.name." + name;
    }
}
