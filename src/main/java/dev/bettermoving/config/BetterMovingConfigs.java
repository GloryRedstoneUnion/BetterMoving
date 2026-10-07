package dev.bettermoving.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.bettermoving.BetterMovingClient;
import dev.bettermoving.physics.VirtualPlatform;
import dev.bettermoving.physics.VoidProtectionPlatform;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
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

    public static final ConfigBooleanHotkeyed SIMULATE_RIPTIDE_ANYWHERE =
            new ConfigBooleanHotkeyed(
                    "simulateRiptideAnywhere",
                    false,
                    "",
                    comment("simulateRiptideAnywhere"),
                    prettyName("simulateRiptideAnywhere"));

    public static final ConfigBooleanHotkeyed CUSTOM_RIPTIDE_CHARGE_TIME =
            new ConfigBooleanHotkeyed(
                    "customRiptideChargeTime",
                    false,
                    "",
                    comment("customRiptideChargeTime"),
                    prettyName("customRiptideChargeTime"));

    public static final ConfigInteger RIPTIDE_CHARGE_TIME_TICKS =
            new ConfigInteger(
                    "riptideChargeTimeTicks",
                    10,
                    0,
                    Integer.MAX_VALUE,
                    false,
                    comment("riptideChargeTimeTicks"));

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

    public static final ConfigBooleanHotkeyed VOID_PROTECTION_PLATFORM =
            new ConfigBooleanHotkeyed(
                    "voidProtectionPlatform",
                    false,
                    "",
                    comment("voidProtectionPlatform"),
                    prettyName("voidProtectionPlatform"));

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

    public static final ConfigBooleanHotkeyed INFINITE_ELYTRA_FIREWORKS =
            new ConfigBooleanHotkeyed(
                    "infiniteElytraFireworks",
                    false,
                    "",
                    comment("infiniteElytraFireworks"),
                    prettyName("infiniteElytraFireworks"));

    public static final ConfigBooleanHotkeyed CANCEL_ELYTRA_FIREWORK_ON_STOP =
            new ConfigBooleanHotkeyed(
                    "cancelElytraFireworkOnStop",
                    false,
                    "",
                    comment("cancelElytraFireworkOnStop"),
                    prettyName("cancelElytraFireworkOnStop"));

    public static final ConfigBooleanHotkeyed ELYTRA_FIREWORK_BLOCK_USE =
            new ConfigBooleanHotkeyed(
                    "elytraFireworkBlockUse",
                    false,
                    "",
                    comment("elytraFireworkBlockUse"),
                    prettyName("elytraFireworkBlockUse"));

    public static final ConfigBooleanHotkeyed BLOCK_NON_ELYTRA_FIREWORK_USE =
            new ConfigBooleanHotkeyed(
                    "blockNonElytraFireworkUse",
                    false,
                    "",
                    comment("blockNonElytraFireworkUse"),
                    prettyName("blockNonElytraFireworkUse"));

    public static final ConfigBooleanHotkeyed SIMULATE_ELYTRA_FIREWORK_SPEED =
            new ConfigBooleanHotkeyed(
                    "simulateElytraFireworkSpeed",
                    false,
                    "",
                    comment("simulateElytraFireworkSpeed"),
                    prettyName("simulateElytraFireworkSpeed"));

    public static final ConfigInteger SIMULATED_SPEED_POTION_LEVEL =
            new ConfigInteger(
                    "simulatedSpeedPotionLevel",
                    0,
                    0,
                    Integer.MAX_VALUE,
                    false,
                    comment("simulatedSpeedPotionLevel"));

    public static final ConfigInteger SIMULATED_JUMP_BOOST_LEVEL =
            new ConfigInteger(
                    "simulatedJumpBoostLevel",
                    0,
                    0,
                    Integer.MAX_VALUE,
                    false,
                    comment("simulatedJumpBoostLevel"));

    public static final ConfigInteger SIMULATED_DOLPHINS_GRACE_LEVEL =
            new ConfigInteger(
                    "simulatedDolphinsGraceLevel",
                    0,
                    0,
                    Integer.MAX_VALUE,
                    false,
                    comment("simulatedDolphinsGraceLevel"));

    public static final ConfigDouble SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED =
            new ConfigDouble(
                    "simulatedElytraFireworkTargetSpeed",
                    64.0,
                    0.0,
                    Double.MAX_VALUE,
                    false,
                    comment("simulatedElytraFireworkTargetSpeed"));

    public static final ConfigBooleanHotkeyed CUSTOM_ELYTRA_FIREWORK_LIFETIME =
            new ConfigBooleanHotkeyed(
                    "customElytraFireworkLifetime",
                    false,
                    "",
                    comment("customElytraFireworkLifetime"),
                    prettyName("customElytraFireworkLifetime"));

    public static final ConfigInteger ELYTRA_FIREWORK_LIFETIME_FLIGHT_1 =
            new ConfigInteger("elytraFireworkLifetimeFlight1", 0, 0, Integer.MAX_VALUE, false,
                    comment("elytraFireworkLifetimeFlight1"));
    public static final ConfigInteger ELYTRA_FIREWORK_LIFETIME_FLIGHT_2 =
            new ConfigInteger("elytraFireworkLifetimeFlight2", 0, 0, Integer.MAX_VALUE, false,
                    comment("elytraFireworkLifetimeFlight2"));
    public static final ConfigInteger ELYTRA_FIREWORK_LIFETIME_FLIGHT_3 =
            new ConfigInteger("elytraFireworkLifetimeFlight3", 0, 0, Integer.MAX_VALUE, false,
                    comment("elytraFireworkLifetimeFlight3"));

    public static final ConfigHotkey OPEN_CONFIG_GUI =
            new ConfigHotkey(
                    "openConfigGui",
                    "L,C",
                    comment("openConfigGui"),
                    prettyName("openConfigGui"));

    public static final ConfigHotkey RESET_MOMENTUM =
            new ConfigHotkey(
                    "resetMomentum",
                    "",
                    KeybindSettings.DEFAULT,
                    comment("resetMomentum"),
                    prettyName("resetMomentum"));

    public static final List<IConfigBase> OPTIONS = List.of(
            IGNORE_FLUID_PHYSICS,
            MODEL,
            VIRTUAL_PLATFORM,
            VOID_PROTECTION_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_RIPTIDE_ANYWHERE,
            CUSTOM_RIPTIDE_CHARGE_TIME,
            RIPTIDE_CHARGE_TIME_TICKS,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            INFINITE_ELYTRA_FIREWORKS,
            CANCEL_ELYTRA_FIREWORK_ON_STOP,
            ELYTRA_FIREWORK_BLOCK_USE,
            BLOCK_NON_ELYTRA_FIREWORK_USE,
            SIMULATE_ELYTRA_FIREWORK_SPEED,
            SIMULATED_SPEED_POTION_LEVEL,
            SIMULATED_JUMP_BOOST_LEVEL,
            SIMULATED_DOLPHINS_GRACE_LEVEL,
            SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED,
            CUSTOM_ELYTRA_FIREWORK_LIFETIME,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_1,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_2,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_3);
    public static final List<ConfigHotkey> STANDALONE_HOTKEYS = List.of(OPEN_CONFIG_GUI, RESET_MOMENTUM);
    public static final List<IHotkey> ALL_HOTKEYS = List.of(
            IGNORE_FLUID_PHYSICS,
            VIRTUAL_PLATFORM,
            VOID_PROTECTION_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_RIPTIDE_ANYWHERE,
            CUSTOM_RIPTIDE_CHARGE_TIME,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            INFINITE_ELYTRA_FIREWORKS,
            CANCEL_ELYTRA_FIREWORK_ON_STOP,
            ELYTRA_FIREWORK_BLOCK_USE,
            BLOCK_NON_ELYTRA_FIREWORK_USE,
            SIMULATE_ELYTRA_FIREWORK_SPEED,
            CUSTOM_ELYTRA_FIREWORK_LIFETIME,
            OPEN_CONFIG_GUI,
            RESET_MOMENTUM);
    public static final List<IConfigBase> GUI_OPTIONS = List.of(
            OPEN_CONFIG_GUI,
            RESET_MOMENTUM,
            IGNORE_FLUID_PHYSICS,
            MODEL,
            VIRTUAL_PLATFORM,
            VOID_PROTECTION_PLATFORM,
            IGNORE_SPRINT_HUNGER,
            IGNORE_SLIPPERY_BLOCKS,
            SIMULATE_RIPTIDE_ANYWHERE,
            CUSTOM_RIPTIDE_CHARGE_TIME,
            SIMULATE_POTION_EFFECTS,
            OVERRIDE_POTION_EFFECTS,
            IGNORE_LEVITATION_AND_SLOWNESS,
            INFINITE_ELYTRA_FIREWORKS,
            CANCEL_ELYTRA_FIREWORK_ON_STOP,
            ELYTRA_FIREWORK_BLOCK_USE,
            BLOCK_NON_ELYTRA_FIREWORK_USE,
            SIMULATE_ELYTRA_FIREWORK_SPEED,
            CUSTOM_ELYTRA_FIREWORK_LIFETIME,
            SIMULATED_SPEED_POTION_LEVEL,
            SIMULATED_JUMP_BOOST_LEVEL,
            SIMULATED_DOLPHINS_GRACE_LEVEL,
            SIMULATED_ELYTRA_FIREWORK_TARGET_SPEED,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_1,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_2,
            ELYTRA_FIREWORK_LIFETIME_FLIGHT_3,
            RIPTIDE_CHARGE_TIME_TICKS);

    public static final BetterMovingConfigs INSTANCE = new BetterMovingConfigs();

    private static final File CONFIG_FILE =
            new File(FileUtils.getConfigDirectory(), "bettermoving.json");
    private static final File LEGACY_CONFIG_FILE =
            new File(FileUtils.getConfigDirectory(), "fluidair.json");

    private BetterMovingConfigs() {
    }

    public static void register() {
        VIRTUAL_PLATFORM.setValueChangeCallback(config -> VirtualPlatform.onOptionChanged());
        VOID_PROTECTION_PLATFORM.setValueChangeCallback(config -> VoidProtectionPlatform.onOptionChanged());
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
