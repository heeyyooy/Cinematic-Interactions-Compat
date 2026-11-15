# Cinematic Interactions Compatibility Addon

Standalone NeoForge addon for Minecraft 1.21.1. It requires the original
**Cinematic Interaction-NeoForge-1.21.1-0.18.30** mod at runtime; it does not
contain, replace, rename, or redistribute that mod.

## Build on Windows

Install a 64-bit Java 21 JDK, extract this project, open PowerShell in the
project directory, and run:

```powershell
.\gradlew.bat clean build
```

The bootstrap script downloads Gradle 9.2.1 into `%USERPROFILE%\\.gradle\\bootstrap`,
verifies its SHA-256 checksum, and runs the build. No system Gradle installation
is required. You can also double-click `build.bat`.

The build output is:

```text
build\\libs\\Cinematic-Interactions-Compat-Addon-NeoForge-1.21.1-1.0.0.jar
```

Only the generated `Cinematic-Interactions-Compat-Addon-...jar` is the addon.
Do **not** put any source archive, Gradle file, or the original Cinematic
Interactions jar into `mods` as a substitute for this build output.

## Install

Put the generated addon JAR in the instance's `mods` folder, alongside:

- Cinematic Interaction-NeoForge-1.21.1-0.18.30
- the supported workstation mods you use

The addon metadata declares Cinematic Interactions 0.18.30 as a required
runtime dependency. Amendments is optional and the supported dependency range
starts at the installed mod version `1.21-2.2.7` (not `2.2.7`). The compatibility mixins target Cinematic Interactions by
class name, so the original JAR is not copied into this source project and is
not included in the resulting addon.

## Compatibility targets

- Carved Wood and Every Compat crafting tables, lecterns and chiseled bookshelves
- Quark deepslate and blackstone furnaces
- Easy Anvils anvils
- Easy Magic enchanting table
- Caverns & Chasms Bejeweled Anvil
- Amendments writable lectern menu payload and lectern BlockEntity subclasses

Easy Magic's and Easy Anvils' own item renderers are suppressed while
Cinematic Interactions is drawing the matching workstation scene. Stored
container items are not changed. During the enchanting-table outro, Easy Magic's
normal table items cross-fade back in over 0.45 seconds while the Cinematic
Interactions item and lapis fade out. This covers both Easy Magic's flat and
floating item layouts. During the anvil outro, the input items stay at their
rest positions instead of flying into the player's inventory.

Easy Magic's separate enchantment clues are also used by the enchanting scene
so its availability tooltip does not incorrectly report limited enchantability.

## Important status

This repository contains buildable source files, not a precompiled mod JAR.
The generated binary has to be produced by running the command above.
