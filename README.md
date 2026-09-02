# Fluid Air

Fluid Air is a client-side Fabric 1.20.1 mod built on malilib. When enabled,
every local-player movement branch treats water, lava, and bubble columns as
air while non-movement effects such as underwater vision and breathing remain
unchanged.

This includes walking, sprinting, jumping, fall-distance tracking, fluid
currents and drag, swimming pose transitions, Elytra activation and flight,
and Riptide eligibility. Rain still enables Riptide exactly as it does in
vanilla.

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

When the optional Mod Menu mod is installed, its configuration button opens
the same MaLiLib configuration screen. Fluid Air works normally when Mod Menu
is not installed.

This mod only changes client-side movement prediction. Servers still evaluate
movement and Elytra state using their own fluid state, so a server or
anti-cheat may reject or correct movement that it considers invalid.
