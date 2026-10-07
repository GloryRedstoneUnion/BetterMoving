# BetterMoving

BetterMoving is a client-only Fabric mod for Minecraft 1.20.1. It provides
configurable movement overrides through a MaLiLib configuration screen,
including fluid movement, invisible platforms, simulated potion effects,
client-side Riptide, and Elytra firework controls.

Current version: `1.13.0` (local build; not published yet).

Install BetterMoving only on the client. It changes local movement prediction,
not server rules. Servers can reject or correct movement, and server-side
damage and item consumption remain authoritative.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.15.11 or newer
- Fabric API 0.92.2 or newer for Minecraft 1.20.1
- MaLiLib 0.16.x
- Java 17 or newer

[Mod Menu](https://modrinth.com/mod/modmenu) is optional. When installed, its
BetterMoving configuration button opens the MaLiLib screen. The configuration
hotkey works without Mod Menu.

## Installation

1. Install Fabric Loader for Minecraft 1.20.1.
2. Place compatible Fabric API and MaLiLib JARs in the instance's `mods` folder.
3. Download an installable JAR from [Releases](https://github.com/GloryRedstoneUnion/BetterMoving/releases),
   or build this version using the instructions below, and place it in the same `mods` folder.
4. Optionally install Mod Menu, then launch Minecraft.

The `-sources.jar` is for source inspection. Do not install it as a mod.

## Configuration

Open the configuration screen with `L + C`, or use the BetterMoving button in
Mod Menu. The `Open configuration screen` hotkey can be rebound in MaLiLib.

All feature toggles are disabled and have no hotkey assigned by default.
Each toggle supports a configurable hotkey. `Movement model` defaults to
`Air rules`; all three simulated potion levels default to `0` and accept
integers from `0` to `Integer.MAX_VALUE`. The model and level settings do not
have hotkeys.

Settings are saved in `config/bettermoving.json`. If that file does not exist,
BetterMoving imports `config/fluidair.json`, when available, and saves the
settings under the current name.

## Features

### Fluid movement

Enable `Ignore fluid physics`, then select a `Movement model`:

- `Air rules` treats water, lava, and bubble columns as air for local movement.
  Walk, sprint, jump, fall, and use an Elytra without fluid drag, swimming
  slowdown, or bubble-column forces.
- `Water rules` treats those fluids as ordinary water. This includes swimming
  poses and sprint-swimming in lava and bubble columns, without bubble-column
  lift or downward drag.

The model applies only while `Ignore fluid physics` is enabled. Fluid
rendering, breathing, fire, damage, underwater vision, and other non-movement
behavior continue to use the real world state.

### Virtual platform

`Virtual platform` records the local player's feet height when enabled and
adds an invisible movement-only floor at that height. Walk, sprint, sneak,
jump, and land on it like ordinary ground; it is not creative flight.

Toggle the option off and on to record a different height. Respawning,
reconnecting, or changing dimension also captures a fresh height.

### Void protection platform

`Void protection platform` adds an invisible movement-only floor at the
current world's void damage boundary:

```java
world.getBottomY() - 64
```

This is the boundary below which vanilla applies void damage, not the minimum
build height itself. The platform stops local downward movement at that
boundary. Its height is recalculated for each player and world, including
dimensions with different minimum build heights.

Both platform options affect only local-player movement collision. They do
not create world blocks, render a platform, change raycasts or interaction
targets, or affect other entities. `Void protection platform` and
`Virtual platform` are independent and can be enabled together. Void
protection does not cancel damage or change the server's position checks.

### Sprint hunger override

`Ignore sprint hunger` allows normal client-side sprinting at food level 6 or
below. It changes only the food check in `ClientPlayerEntity.canSprint()`.
Actual food, saturation, movement speed, and other sprint requirements are
unchanged.

### Slippery blocks

`Ignore slippery blocks` treats ice and other blocks with above-normal
slipperiness as ordinary blocks during local-player movement. It does not
change block states, rendering, interactions, or other entities.

### Riptide anywhere

`Simulate Riptide anywhere` simulates Riptide-enchanted trident use for the
local player in any environment, including dry air and water. Both hands
use vanilla charging: releasing after at least 10 ticks applies the normal
enchantment-dependent impulse, ground lift, sound, and 20-tick spin. Shorter
charges do not launch the player. Vanilla cooldown and durability checks
still apply.

Simulated use sends neither an item-use packet nor a release-use packet and
does not consume trident durability. Aiming at an ordinary block also skips
the unused block-use fallback packet; accepted container and other block
interactions retain their normal behavior. Disabling the option or changing
the held item cancels an in-progress local charge. Incoming server status
updates do not interrupt a valid local charge.

The toggle is disabled and unbound by default and works independently of
`Ignore fluid physics`, including both movement models. Tridents without
Riptide retain vanilla throwing and packets. Other players and server-side
combat remain unchanged; servers can still correct the simulated movement.

### Simulated potion effects

Enable `Simulate potion effects` to use the configured potion levels in
local-player movement calculations. No real status effects, particles, HUD
icons, or server-side effects are added.

| Setting | Movement behavior |
| --- | --- |
| `Simulated speed potion level` | Uses the vanilla Speed multiplier: `1 + 0.2 * level`. |
| `Simulated jump boost level` | Uses the vanilla Jump Boost formula, adding `0.1 * level` to jump velocity. |
| `Simulated dolphin's grace level` | Enables vanilla Dolphin's Grace water movement at any level above `0`; vanilla does not scale it with the amplifier. |

Level `0` represents no simulated effect. With
`Override existing potion effects` disabled, stronger existing Speed and
Jump Boost levels and any existing Dolphin's Grace effect are preserved.
With that toggle enabled, the configured levels replace the corresponding
real effects for movement calculations, including a configured level of `0`.
The override applies only while `Simulate potion effects` is enabled.

### Levitation and slowness

`Ignore levitation and slowness` controls both movement overrides:

- Levitation does not add vertical motion or block Elytra activation and
  gliding.
- Slowness does not reduce local movement speed. Other attribute modifiers
  continue to apply.

Real effects remain active for particles, icons, and non-movement behavior.
While real Levitation is present, BetterMoving can retain a started local
Elytra flight when the server clears its gliding flag. This does not enable
server-side gliding or bypass movement validation.

The local flight override ends on landing, riding, creative flight, climbing,
death, water movement, missing or unusable Elytra, an explicit flight stop,
disconnecting, disabling the option, or removal of Levitation. Air-rule fluid
movement and local firework boosts remain compatible.

### Elytra fireworks

`Infinite Elytra fireworks` simulates a firework rocket while the local player
is gliding, applying vanilla acceleration without sending the item-use packet
or consuming a rocket on the server. The client-side rocket expires after its
normal lifetime. This option does not create rockets outside Elytra flight.

`Elytra firework block use` routes firework use on a block or wall to an Elytra
boost while gliding, instead of launching a rocket from the block. With
`Infinite Elytra fireworks` enabled, the boost is simulated without rocket
consumption. Otherwise, vanilla item-use packets and rocket consumption are
preserved.

`Block non-Elytra firework use` blocks firework launches from blocks or walls
unless the player is gliding with `Elytra firework block use` enabled. It
blocks only the actual launch path: accepted workbench and container
interactions still work, and entity interactions are unaffected. Sneaking
past a block's normal interaction is blocked if it would launch a rocket.

`Cancel Elytra fireworks on glide stop` removes client-only simulated rockets
when the local player stops gliding, but only while `Infinite Elytra fireworks`
is enabled. It also handles glide state changes received through tracked entity
data, so a canceled glide cannot leave an old simulated rocket to accelerate a
later glide. The setting is disabled and unbound by default.

A blocked launch sends no interaction packet, consumes no rocket, creates no
rocket entity, and does not swing the hand. Aiming at air is unaffected.
These firework options are independent, hotkey-capable toggles.

`Simulate Elytra firework target speed` adjusts the local Elytra firework
acceleration toward a configurable equilibrium speed. The companion
`Simulated Elytra firework target speed (m/s)` value is expressed in meters per
second and is converted to per-tick speed with `v_t = V / 20`. For example,
`40` targets `2.0` meters per tick. Values from `0` through the Java double
maximum (`Double.MAX_VALUE`) are accepted. The default value is `64 m/s`
(`3.2` meters per tick). The toggle is disabled and unbound by default. The
setting only changes the local player's Elytra firework propulsion and leaves
vanilla behavior unchanged while disabled.

`Custom Elytra firework lifetime` replaces the boost duration for Elytra
fireworks with Flight 1, 2, or 3 using the corresponding tick value. Flight
levels 0 and 4 or higher, non-Elytra launches, and rockets while the option is
disabled retain their vanilla lifetime. The Flight 1, 2, and 3 values accept
any non-negative `int` value up to `Integer.MAX_VALUE` ticks. It also applies to rockets created by
`Infinite Elytra fireworks`; rocket consumption behavior is unchanged. The
toggle is disabled and unbound by default, and all three values default to
`0` ticks.

## Building and testing

Build the mod, run unit tests, and compile the client compatibility suite:

```sh
./gradlew build compatTestClasses
```

On Windows, use `gradlew.bat` instead of `./gradlew`. The installable JAR is
`build/libs/bettermoving-1.13.0.jar`; the `-sources.jar` is for source
inspection only.

Run the disposable client compatibility suite:

```sh
./gradlew runClient \
  -PbettermovingCompatTest \
  -PbettermovingMovementCompatTest \
  -PbettermovingPlatformCompatTest \
  -PbettermovingSprintCompatTest \
  -PbettermovingSlipperinessCompatTest \
  -PbettermovingPotionEffectsCompatTest \
  -PbettermovingElytraFireworksCompatTest \
  -PbettermovingRiptideCompatTest
```

Add `-PbettermovingModMenuTest` to include optional Mod Menu integration.
The suite covers configuration persistence, fluid models, both platforms,
sprint restrictions, slippery-block friction, simulated potion formulas,
Levitation and Slowness overrides, Elytra firework behavior, and local Riptide.

Flight checks reproduce integrated-server Levitation rejection and verify
local gliding, stopping conditions, remote-player isolation, fluid models,
and firework acceleration. Firework checks cover both hands in Creative and
Survival, block and entity interactions, sneaking fallbacks, outgoing packets,
and block prediction cleanup.

Riptide checks cover both hands, Creative and Survival, vanilla charge
thresholds and impulse levels, ground lift, natural ticking and spin poses,
spin expiration and collision, server status updates, cancellation, cooldown
and durability restrictions, water and fluid-model compatibility, outgoing
packets, accepted block and entity interactions, and vanilla behavior for
unenchanted tridents and remote players.

## License

BetterMoving is licensed under the MIT License. See [LICENSE](LICENSE).
