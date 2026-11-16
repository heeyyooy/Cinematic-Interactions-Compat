# Cinematic Interactions Compat

Unofficial compatibility addon for **Cinematic Interactions**. It extends the original workstation scenes to additional modded blocks and improves Easy Magic and Easy Anvils integration.

- **Minecraft:** 1.21.1
- **Mod loader:** NeoForge 21.1.x
- **Java:** 21
- **Addon version:** 1.0.0
- **Required:** Cinematic Interactions 0.18.30

The addon is separate from the original Cinematic Interactions mod. It does not include or redistribute the original mod or any of its dependencies.

## Features

### Easy Magic
- Prevents duplicate enchanting-table item renders while the cinematic scene is active.
- Uses Easy Magic's enchantment clues for the scene's availability display.
- Adds a physical reroll token to the scene, using Easy Magic's native reroll action and configured resource costs.
- Keeps the enchanting-table item and lapis visuals during the scene transition.

### Easy Anvils
- Prevents duplicate anvil item renders during the cinematic scene.
- Keeps the same central 3D item layout during the scene, the outro, and after the scene ends.
- Respects Easy Anvils' client option for rendering anvil contents.

### Other workstation integrations
- **Carved Wood and Every Compat:** crafting tables, lecterns, and chiseled bookshelves.
- **Quark:** deepslate and blackstone furnaces.
- **Caverns & Chasms:** Bejeweled Anvil.
- **Amendments:** writable lectern menu data and lectern block-entity compatibility.

These integrations are optional where the corresponding mod is not a declared required dependency. The source targets Minecraft 1.21.1 and the class names/APIs used by the listed mod versions; compatibility with other Minecraft or mod versions is not implied.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Cinematic Interactions 0.18.30

Amendments is optional. The metadata declares the supported Amendments range as `1.21-2.2.7` and newer within its compatible release line.

Easy Magic, Easy Anvils, Carved Wood, Every Compat, Quark, and Caverns & Chasms are optional integrations; install the mods whose workstation features you want to use.

## Build on Windows

Install a 64-bit Java 21 JDK. Open PowerShell in the repository directory and run:

```powershell
.\gradlew.bat clean build
```

The included bootstrap script downloads the pinned Gradle 9.2.1 distribution to `%USERPROFILE%\.gradle\bootstrap`, verifies its SHA-256 checksum, and then runs the build. A separate system-wide Gradle installation is not required. You can also double-click `build.bat`.

The compiled addon is written to:

```text
build\libs\Cinematic-Interactions-Compat-1.0.0.jar
```

Copy that JAR into the Minecraft instance's `mods` folder alongside Cinematic Interactions and any optional workstation mods you use.

## Project status

This repository contains source code and build scripts, not a prebuilt JAR or a published release. The compatibility hooks target the versions listed above; perform an in-game test with your exact mod set before using the addon in an established world.

## License

MIT. See [LICENSE](LICENSE).
