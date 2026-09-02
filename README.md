# Fluid Air

Fluid Air is a client-side Fabric 1.20.1 mod built on malilib. When enabled,
the local player's movement treats water and lava like air and ignores
bubble-column lift and drag.

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

This mod only changes client-side movement prediction. A server or anti-cheat
may correct movement that it considers invalid.
