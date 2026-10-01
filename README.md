# BetterMoving

BetterMoving is a client-side Fabric mod for Minecraft 1.20.1. It adds
configurable fluid movement, an invisible movement-only platform, an optional
client-side sprint hunger override, normal-friction movement on slippery
blocks, and simulated potion movement effects. The configuration is provided by
[MaLiLib](https://github.com/maruohon/malilib).

## 1.8.0 release

This release replaces final-velocity scaling with client-side simulation of
vanilla potion movement effects. `Simulated speed potion level` uses the same
horizontal movement calculation as Speed, while `Simulated jump boost level`
uses the same jump calculation as Jump Boost. Both are controlled by the
toggleable `Simulate potion effects` option. Existing stronger effects and
other movement modifiers remain part of the calculation.

## Features

### Fluid movement

Enable `Ignore fluid physics` to replace the local player's fluid movement
queries with one of two selectable models:

- `Air rules` treats water, lava, and bubble columns as air for movement. The
  player can walk, sprint, jump, fall, use an Elytra, and move through fluids
  without fluid drag, swimming slowdown, or bubble-column lift.
- `Water rules` treats water, lava, and bubble columns as ordinary water for
  movement. Lava uses the complete water movement path, including swimming
  pose transitions and sprint-swimming. Bubble-column lift and drag are
  suppressed.

The model only affects client-side movement prediction. Fluid rendering,
underwater vision, breathing, fire, lava damage, and other non-movement effects
continue to use the real world state.

### Virtual platform

`Virtual platform` creates an invisible, movement-only horizontal floor at the
local player's captured feet height. It behaves like ordinary ground for
walking, sprinting, sneaking, jumping, and landing.

The platform:

- affects only the local player's movement collision;
- does not create blocks or change rendering, raycasts, interaction targets, or
  other entities;
- preserves real walls, ceilings, and steps; and
- captures a fresh height after toggling, respawning, reconnecting, or changing
  dimension.

It is not creative flight and does not enable flight abilities.

### Sprint hunger override

`Ignore sprint hunger` allows the local player to start and maintain normal
sprinting at food level 6 or below. It changes only the food-level expression
inside `ClientPlayerEntity.canSprint()`.

Actual food, saturation, movement speed, velocity, and all other vanilla sprint
requirements remain unchanged. Forward input, blindness, item use, collision,
and pose restrictions still apply.

### Slippery blocks

`Ignore slippery blocks` treats ice and other blocks with above-normal
slipperiness as ordinary blocks during local-player movement. It changes only
the slipperiness value read by the client's movement calculation; block states,
rendering, interactions, and other entities remain unchanged.

### Simulated potion effects

Enable `Simulate potion effects` to make local-player movement calculations act
as though the player has the configured potion effects. The toggle is disabled
and unbound by default. The effect is client-side and does not add a real status
effect, potion particles, HUD icons, or server-side effects.

- `Simulated speed potion level` controls horizontal acceleration using the
  vanilla Speed formula. Level `0` adds no effect, level `1` matches Speed I,
  and level `n` applies the vanilla `1 + 0.2 * n` total movement-speed
  multiplier.
- `Simulated jump boost level` controls jump velocity using the vanilla Jump
  Boost formula. Level `0` adds no effect, level `1` matches Jump Boost I,
  and each additional level adds another `0.1` to jump velocity.

Both values range from `0` to `255`. Existing stronger Speed or Jump Boost
effects are preserved, and the simulated level raises the local calculation to
at least the configured level. Other movement modifiers are still included
before the simulated potion calculation.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.15.11 or newer
- Fabric API 0.92.2 or newer for Minecraft 1.20.1
- MaLiLib 0.16.x
- Java 17 or newer

Mod Menu is optional. When installed, its BetterMoving configuration button
opens the same MaLiLib configuration screen. BetterMoving does not require Mod
Menu to run.

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 1.20.1.
2. Install MaLiLib 0.16.x.
3. Download `bettermoving-1.8.0.jar` from the
   [Releases](https://github.com/GloryRedstoneUnion/BetterMoving/releases)
   page and place it in the `mods` folder.
4. Optionally install Mod Menu for an in-game configuration button.

BetterMoving is a client-only mod. It does not need to be installed on a
server, although a server or anti-cheat may correct movement that it considers
invalid.

## Configuration

Open the MaLiLib configuration screen with `L`, then `C`, or use the
`Open configuration screen` hotkey. The following toggle options are disabled
and unbound by default:

- `Ignore fluid physics`
- `Virtual platform`
- `Ignore sprint hunger`
- `Ignore slippery blocks`
- `Simulate potion effects`

`Simulated speed potion level` and `Simulated jump boost level` default to `0`
and have no hotkeys. They are applied only while `Simulate potion effects` is
enabled.

Assign toggle hotkeys directly in the configuration screen. `Movement model`
defaults to `Air rules` and is used only while `Ignore fluid physics` is
enabled.

Configuration is stored in `config/bettermoving.json`. If that file does not
exist, BetterMoving imports `config/fluidair.json` and saves the migrated
settings under the new name.

## Client-side limitations

The mod changes local movement prediction only. The server still evaluates
the player's real fluid state, hunger, collisions, and Elytra state. On a
server that validates movement, the server can reject or correct a movement
that is not allowed by its own rules.

## Development and testing

Build the mod and run unit tests with:

```text
./gradlew build compatTestClasses
```

On Windows, use `gradlew.bat`. The disposable client compatibility suite can be
run with:

```text
./gradlew runClient \
  -PbettermovingCompatTest \
  -PbettermovingPlatformCompatTest \
  -PbettermovingMovementCompatTest \
  -PbettermovingSprintCompatTest \
  -PbettermovingSlipperinessCompatTest \
  -PbettermovingPotionEffectsCompatTest
```

Add `-PbettermovingModMenuTest` to include the optional Mod Menu integration.
The suite checks fluid modes, virtual-platform collision behavior, sprint
movement, food levels 0 through 20, hotkey/config persistence, and vanilla
sprint restrictions, as well as normal-friction movement on slippery blocks.
The potion-effects compatibility probe checks the toggle, level defaults and
persistence, hotkey registration, Speed and Jump Boost formulas, and disabled
behavior.

## License

BetterMoving is licensed under the MIT License. See [LICENSE](LICENSE).
