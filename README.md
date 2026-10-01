# BetterMoving

BetterMoving is a client-side Fabric 1.20.1 mod built on malilib. When enabled, it
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

When the optional Mod Menu mod is installed, its configuration button opens
the same MaLiLib configuration screen. BetterMoving works normally when Mod Menu
is not installed.

BetterMoving replaces Fluid Air and uses the `bettermoving` mod ID. When
upgrading, replace the old jar with `bettermoving-1.3.2.jar`. Configuration is
stored in `config/bettermoving.json`. If that file does not exist, BetterMoving
imports the existing `config/fluidair.json`, including movement settings and
hotkeys, and saves them under the new name.

This mod only changes client-side movement prediction. Servers still evaluate
movement and Elytra state using their own fluid state, so a server or
anti-cheat may reject or correct movement that it considers invalid.
