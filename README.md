# BetterMoving

BetterMoving is a client-side Fabric 1.20.1 mod built on malilib, with configurable
fluid movement, an invisible virtual platform, and a sprint hunger override.

When `Ignore fluid physics` is enabled, it
applies the selected movement model to water, lava, and bubble columns for the
local player. Non-movement effects such as underwater vision, breathing, and
lava damage remain unchanged.

This includes walking, sprinting, jumping, fall-distance tracking, fluid
currents and drag, swimming pose transitions, Elytra activation and flight,
and Riptide eligibility.

Water rules expose a scoped virtual fluid state to the vanilla movement code:
while the local player is evaluating movement, lava contact, submersion, and
fluid height are resolved through the same state queries used for water. The
scope ends when the movement call returns, so lava damage, fire, breathing,
underwater vision, and other non-movement behavior continue to use the real
world state.

The available movement models are:

- `Air rules` treats water, lava, and bubble columns as air. Rain still enables
  Riptide exactly as it does in vanilla.
- `Water rules` treats all three as ordinary water. Lava supports the complete
  water movement path, including sprint-swimming and the swimming pose without
  the crawling input slowdown, while bubble-column lift and drag are suppressed.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.15.11 or newer
- Fabric API 0.92.2 or newer for Minecraft 1.20.1
- malilib 0.16.x
- Java 17 or newer

## Usage

Press `L`, then `C` to open the malilib configuration screen. The
`Ignore fluid physics` option is disabled by default. Its toggle hotkey is
unbound by default and can be assigned directly in the configuration screen.
The `Movement model` option defaults to `Air rules` and only changes behavior
while `Ignore fluid physics` is enabled.

`Virtual platform` is an independent toggle, disabled and unbound by default.
Enabling it captures the exact Y coordinate at the bottom of the player's
bounding box and creates a horizontal, one-block-thick collision layer with
its top at that height. The layer supports normal walking, sprinting, sneaking,
jumping, and landing. Its height stays fixed while moving or jumping; toggle
it off and on to capture a different height. Turning it off restores ordinary
falling. If enabled before joining a world, it captures the height once the
player becomes available. Respawning, reconnecting, or changing dimension
captures a fresh height for the new player or world.

The platform affects only the local player's movement collisions and ledge
checks. It creates no world blocks, rendering, raycast targets, or block
interaction changes, and does not affect other entities or the server. Real
walls, ceilings, and steps still participate in collision. It does not enable
flight. The fluid movement option and model continue to work independently.

`Ignore sprint hunger` is another independent toggle, disabled and unbound by
default. It lets the local client player start and continue sprinting at food
level 6 or below. Only the food-level query inside `ClientPlayerEntity.canSprint`
is evaluated as full hunger. Actual food and saturation remain unchanged, and
vanilla controls sprint movement and speed. Other sprint requirements, such as
forward input, blindness, item use, and collision checks, still apply. Turning
it off immediately restores the normal hunger requirement on the next movement
tick. Assign its toggle hotkey in the same MaLiLib configuration screen.

When the optional Mod Menu mod is installed, its configuration button opens
the same MaLiLib configuration screen. BetterMoving works normally when Mod Menu
is not installed.

BetterMoving replaces Fluid Air and uses the `bettermoving` mod ID. When
upgrading, replace the old jar with `bettermoving-1.5.0.jar`. Configuration is
stored in `config/bettermoving.json`. If that file does not exist, BetterMoving
imports the existing `config/fluidair.json`, including movement settings and
hotkeys, and saves them under the new name.

This mod only changes client-side movement prediction. Servers still evaluate
movement and Elytra state using their own fluid state, so a server or
anti-cheat may reject or correct movement that it considers invalid.

## Validation

Run `./gradlew build compatTestClasses` for the build and unit tests. The runtime
probe can be launched with `./gradlew runClient -PbettermovingCompatTest
-PbettermovingPlatformCompatTest -PbettermovingMovementCompatTest` (use
`gradlew.bat` on Windows). It creates a disposable single-player test world,
compares 32 ticks of walking, sprinting, sneaking, and sprint-jumping against
real stone ground, and checks fixed-height capture, disabling, session reset,
fast diagonal movement, real obstacles, raycast and entity isolation, and
both fluid movement modes. Add `-PbettermovingModMenuTest` to include Mod Menu.
Add `-PbettermovingSprintCompatTest` to check every food level from 0 to 20,
compare low-food sprint movement with vanilla full-food sprinting, verify
toggle and hotkey persistence, and retain the other vanilla sprint restrictions.
The test client must have accessibility onboarding completed, a render
distance of at least 12 chunks, and `pauseOnLostFocus:false` in `options.txt`.
