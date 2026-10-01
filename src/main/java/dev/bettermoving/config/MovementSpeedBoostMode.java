package dev.bettermoving.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum MovementSpeedBoostMode implements IConfigOptionListEntry {
    HORIZONTAL("horizontal", "bettermoving.config.value.movementSpeedBoostMode.horizontal"),
    VERTICAL("vertical", "bettermoving.config.value.movementSpeedBoostMode.vertical"),
    ALL_AXES("all_axes", "bettermoving.config.value.movementSpeedBoostMode.allAxes");

    private static final MovementSpeedBoostMode[] VALUES = values();

    private final String stringValue;
    private final String translationKey;

    MovementSpeedBoostMode(String stringValue, String translationKey) {
        this.stringValue = stringValue;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue() {
        return this.stringValue;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public MovementSpeedBoostMode cycle(boolean forward) {
        int direction = forward ? 1 : -1;
        return VALUES[Math.floorMod(this.ordinal() + direction, VALUES.length)];
    }

    @Override
    public MovementSpeedBoostMode fromString(String value) {
        for (MovementSpeedBoostMode mode : VALUES) {
            if (mode.stringValue.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return HORIZONTAL;
    }
}
