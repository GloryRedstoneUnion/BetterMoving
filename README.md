# BetterMoving

BetterMoving is a client-side Fabric mod for Minecraft 1.20.1. It changes
local movement calculations through a MaLiLib configuration screen. The
current development version is **1.11.0** (not yet published). The latest
published release is
**[1.10.4](https://github.com/GloryRedstoneUnion/BetterMoving/releases/tag/v1.10.4)**.

## 1.11.0 changes

Adds `Void protection platform`, a disabled and hotkey-capable client-only
movement option. When enabled, it creates an invisible movement-only floor at
the current world's minimum build height, preventing the local player from
falling below that world's void boundary. The height is resolved separately
for each player and world, so dimensions with different minimum heights are
handled correctly.

The void platform does not create blocks, affect rendering or interaction
targets, or affect other entities. It is independent from `Virtual platform`;
both options can be configured and toggled separately.

## 1.10.4 release highlights

Fixes interrupted Elytra flight with `Ignore levitation and slowness` enabled
while the player has real Levitation. Vanilla servers reject the flight and
synchronize a cleared Elytra flag; BetterMoving now retains the local flight
session while ignoring Levitation instead of letting that update interrupt it.

Landing, riding, creative flight, death, water movement, missing or unusable
Elytra, explicit flight stops, disconnecting, disabling the option, and effect
removal end the local override. Air-rule fluid movement and Elytra firework
boosts remain compatible. Other entities and incoming metadata are unchanged.

BetterMoving does not need to be installed on a server. Server-side movement
validation still applies, so a server can correct movement that it considers
invalid.

## Features

### Fluid movement

Enable `Ignore fluid physics` and choose a `Movement model`:

- `Air rules` treats water, lava, and bubble columns as air for movement. The
  local player can walk, sprint, jump, fall, fly with an Elytra, and move
  through fluids without fluid drag, swimming slowdown, or bubble-column lift.
- `Water rules` routes water, lava, and bubble columns through the ordinary
  water movement path. This includes swimming poses and sprint-swimming in
  lava while suppressing bubble-column lift and drag.

Only client-side movement prediction is changed. Fluid rendering, breathing,
fire, damage, underwater vision, and other non-movement effects use the real
world state.

### Virtual platform

`Virtual platform` records the local player's feet height and creates an
invisible movement-only floor at that height. It behaves like ordinary ground
for walking, sprinting, sneaking, jumping, and landing.

The platform does not create blocks, affect rendering, change raycasts or
interaction targets, or affect other entities. It is not creative flight. A
new height is captured after toggling the option, respawning, reconnecting, or
changing dimension.

### Sprint hunger override

`Ignore sprint hunger` lets the local player start and maintain normal
sprinting at food level 6 or below. It changes only the food-level check inside
`ClientPlayerEntity.canSprint()`; all other sprint requirements remain intact.

### Slippery blocks

`Ignore slippery blocks` treats ice and other blocks with above-normal
slipperiness as ordinary blocks during local-player movement. Block states,
rendering, interactions, and other entities are unchanged.

### Simulated potion effects

Enable `Simulate potion effects` to apply configured Speed, Jump Boost, and
Dolphin's Grace levels to local-player movement calculations without adding
real status effects, particles, HUD icons, or server-side effects.

- `Simulated speed potion level` uses the vanilla Speed formula. Level `0` has
  no effect; level `n` applies a `1 + 0.2 * n` movement-speed multiplier.
- `Simulated jump boost level` uses the vanilla Jump Boost formula. Level `0`
  has no effect; each level adds `0.1` to jump velocity.
- `Simulated dolphin's grace level` applies the vanilla Dolphin's Grace water
  movement behavior. Level `0` has no effect; any level from `1` to `255`
  matches the vanilla effect, which does not scale with the amplifier.
- `Override existing potion effects` replaces the local player's existing
  Speed, Jump Boost, and Dolphin's Grace effects with the configured levels
  while simulation is enabled. When it is disabled, stronger existing Speed
  and Jump Boost levels and any existing Dolphin's Grace effect are preserved.

All simulated level values range from `0` to `255`.

### Levitation and slowness

`Ignore levitation and slowness` is a shared, hotkey-capable movement toggle.
When enabled for the local player:

- Levitation does not add vertical movement or block Elytra activation and
  flight.
- Slowness does not reduce movement speed; other attribute modifiers continue
  to apply.

The actual status effects remain active for rendering, particles, icons, and
other non-movement behavior.

While real Levitation is present, an already-started local Elytra flight can
continue even if the server clears its gliding flag. This does not enable
server-side gliding or bypass server movement validation. Ending the effect or
disabling the option returns flight decisions to the synchronized vanilla state.

### Elytra fireworks

`Elytra firework block use` is a shared, hotkey-capable movement toggle. When
enabled while the local player is Elytra gliding, using a firework rocket on a
block or wall routes the action to Elytra acceleration instead of the block-use
path. With `Infinite Elytra fireworks` enabled, the local rocket is simulated
without sending the item-use packet or consuming the server-side rocket.
When `Infinite Elytra fireworks` is disabled, the ordinary vanilla item-use
packet and rocket consumption are preserved.

`Infinite Elytra fireworks` is a shared, hotkey-capable movement toggle. When
enabled while the local player is Elytra gliding, using a firework rocket
creates a client-side rocket and applies the same acceleration as vanilla.
The client does not send the firework item-use packet, so the server does not
consume a rocket. No rocket is created when the player is not fall-flying.

The local rocket is temporary and is removed after its normal lifetime. This
feature changes client-side movement prediction only; the server may still
correct movement according to its own state.

`Block non-Elytra firework use` is a shared, hotkey-capable safety toggle. When
enabled, it blocks only a firework rocket use that reaches the actual launch
path on a block or wall, unless the local player is Elytra gliding and
`Elytra firework block use` is also enabled. Accepted block interactions such as
workbenches and containers remain available, and entity interactions are
unaffected. Sneaking past a block's normal interaction is still blocked when
it would launch a rocket instead. Blocked launches send no interaction packet,
consume no rocket, create no rocket entity, and do not swing the hand. Aiming
at air is unaffected.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.15.11 or newer
- Fabric API 0.92.2 or newer for Minecraft 1.20.1
- MaLiLib 0.16.x
- Java 17 or newer

Mod Menu is optional. When installed, its BetterMoving button opens the same
MaLiLib configuration screen. BetterMoving does not require Mod Menu.

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 1.20.1.
2. Install MaLiLib 0.16.x.
3. Download
   [bettermoving-1.10.4.jar](https://github.com/GloryRedstoneUnion/BetterMoving/releases/download/v1.10.4/bettermoving-1.10.4.jar)
   from the
   [1.10.4 release](https://github.com/GloryRedstoneUnion/BetterMoving/releases/tag/v1.10.4)
   and place it in the `mods` folder. The release also includes
   [bettermoving-1.10.4-sources.jar](https://github.com/GloryRedstoneUnion/BetterMoving/releases/download/v1.10.4/bettermoving-1.10.4-sources.jar)
   for source inspection; it is not an installable mod.
4. Optionally install Mod Menu for an in-game configuration button.

## Configuration

Open the MaLiLib configuration screen with `L`, then `C`, or use the
`Open configuration screen` hotkey. The following toggles are disabled and
unbound by default:

- `Ignore fluid physics`
- `Virtual platform`
- `Void protection platform`
- `Ignore sprint hunger`
- `Ignore slippery blocks`
- `Simulate potion effects`
- `Override existing potion effects`
- `Ignore levitation and slowness`
- `Elytra firework block use`
- `Block non-Elytra firework use`
- `Infinite Elytra fireworks`

`Movement model` defaults to `Air rules` and is used only while `Ignore fluid
physics` is enabled. The simulated Speed, Jump Boost, and Dolphin's Grace
levels default to `0` and do not have hotkeys.

Configuration is stored in `config/bettermoving.json`. If that file does not
exist, BetterMoving imports `config/fluidair.json` and saves the migrated
settings under the new name.

## Development and testing

Build the mod and run unit tests with:

```text
./gradlew build compatTestClasses
```

The installable development JAR is `build/libs/bettermoving-1.11.0.jar`;
the `-sources.jar` is for source inspection only.

On Windows, use `gradlew.bat`. Run the complete disposable client suite with:

```text
./gradlew runClient \
  -PbettermovingCompatTest \
  -PbettermovingMovementCompatTest \
  -PbettermovingPlatformCompatTest \
  -PbettermovingSprintCompatTest \
  -PbettermovingSlipperinessCompatTest \
  -PbettermovingPotionEffectsCompatTest \
  -PbettermovingElytraFireworksCompatTest
```

Add `-PbettermovingModMenuTest` to include the optional Mod Menu integration.
The suite checks fluid modes, virtual-platform and void-protection collision behavior, sprint
movement and restrictions, slippery-block friction, potion formulas,
configuration persistence, the Levitation and Slowness movement override, and
Elytra firework block-use routing, non-consuming Elytra firework boosts, and
non-Elytra firework launch suppression. Levitation checks reproduce a real
vanilla integrated-server flight rejection, replay its metadata through the
client packet handler, and verify sustained flight, stopping conditions,
remote-player isolation, fluid modes, and local firework acceleration.
Firework checks cover both hands in Creative and Survival modes, workbench
and container interactions, sneaking
fallbacks, entity interactions, outgoing interaction packets, and block
prediction cleanup.

## License

BetterMoving is licensed under the MIT License. See [LICENSE](LICENSE).
