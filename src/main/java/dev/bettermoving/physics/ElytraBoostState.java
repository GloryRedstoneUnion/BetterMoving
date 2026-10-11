package dev.bettermoving.physics;

/** Reads the actual Riptide timer instead of a possibly stale tracked spin flag. */
public interface ElytraBoostState {
    boolean bettermoving$hasRiptidePropulsion();
}
