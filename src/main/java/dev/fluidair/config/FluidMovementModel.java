package dev.fluidair.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum FluidMovementModel implements IConfigOptionListEntry {
    AIR("air", "fluidair.config.value.model.air"),
    WATER("water", "fluidair.config.value.model.water");

    private static final FluidMovementModel[] VALUES = values();

    private final String stringValue;
    private final String translationKey;

    FluidMovementModel(String stringValue, String translationKey) {
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
    public FluidMovementModel cycle(boolean forward) {
        int direction = forward ? 1 : -1;
        return VALUES[Math.floorMod(this.ordinal() + direction, VALUES.length)];
    }

    @Override
    public FluidMovementModel fromString(String value) {
        for (FluidMovementModel model : VALUES) {
            if (model.stringValue.equalsIgnoreCase(value)) {
                return model;
            }
        }
        return AIR;
    }
}
