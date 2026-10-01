package dev.bettermoving.physics;

import dev.bettermoving.config.BetterMovingConfigs;
import dev.bettermoving.config.FluidMovementModel;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

public final class FluidMovementContext {
    private static final double EYE_FLUID_CHECK_OFFSET = 0.1111111119389534;
    private static final ThreadLocal<Deque<Entity>> MOVEMENT_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FluidMovementContext() {
    }

    public static void enter(Entity entity) {
        MOVEMENT_STACK.get().push(entity);
    }

    public static void exit(Entity entity) {
        Deque<Entity> stack = MOVEMENT_STACK.get();
        if (stack.isEmpty() || stack.pop() != entity) {
            stack.clear();
        }
        if (stack.isEmpty()) {
            MOVEMENT_STACK.remove();
        }
    }

    public static boolean resolveWaterState(
            Entity entity,
            boolean detectedWaterState,
            boolean detectedLavaState) {
        FluidMovementModel model = activeModel(entity);
        if (model == null) {
            return detectedWaterState;
        }
        return model == FluidMovementModel.WATER
                && (detectedWaterState || detectedLavaState);
    }

    public static boolean resolveSubmergedWaterState(
            Entity entity,
            boolean detectedWaterState) {
        FluidMovementModel model = activeModel(entity);
        if (model == null) {
            return detectedWaterState;
        }
        return model == FluidMovementModel.WATER
                && (isCurrentlySubmerged(entity, FluidTags.WATER)
                || isCurrentlySubmerged(entity, FluidTags.LAVA));
    }

    public static boolean resolveLavaState(Entity entity, boolean detectedLavaState) {
        return activeModel(entity) == null && detectedLavaState;
    }

    public static double resolveFluidHeight(
            Entity entity,
            TagKey<Fluid> fluid,
            double detectedHeight,
            double detectedWaterHeight,
            double detectedLavaHeight) {
        FluidMovementModel model = activeModel(entity);
        if (model == null) {
            return detectedHeight;
        }
        if (fluid.equals(FluidTags.WATER)) {
            return model == FluidMovementModel.WATER
                    ? Math.max(detectedWaterHeight, detectedLavaHeight)
                    : 0.0;
        }
        if (fluid.equals(FluidTags.LAVA)) {
            return 0.0;
        }
        return detectedHeight;
    }

    private static FluidMovementModel activeModel(Entity entity) {
        Deque<Entity> stack = MOVEMENT_STACK.get();
        if (stack.isEmpty()
                || stack.peek() != entity
                || !(entity instanceof ClientPlayerEntity)
                || !BetterMovingConfigs.ignoreFluidPhysics()) {
            return null;
        }
        return BetterMovingConfigs.movementModel();
    }

    private static boolean isCurrentlySubmerged(Entity entity, TagKey<Fluid> fluid) {
        double eyeHeight = entity.getEyeY() - EYE_FLUID_CHECK_OFFSET;
        BlockPos pos = BlockPos.ofFloored(entity.getX(), eyeHeight, entity.getZ());
        FluidState state = entity.getWorld().getFluidState(pos);
        return (double) pos.getY() + state.getHeight(entity.getWorld(), pos) > eyeHeight
                && state.isIn(fluid);
    }
}
