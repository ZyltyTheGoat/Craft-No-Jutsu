# Repository Guidelines

## Project Structure & Module Organization

This is an MCreator-generated NeoForge mod workspace for the `naruto` mod ID. Java sources live under `src/main/java/net/mcreator/naruto`, grouped by role: `procedures` for gameplay logic, `item` and `entity` for content classes, `network` for packets and synced data, `mixin` for mixin hooks, `client` for client-only rendering, and `init` for registries. Runtime resources live in `src/main/resources`, including `META-INF/neoforge.mods.toml`, `naruto.mixins.json`, language files, models, particles, shaders, sounds, and textures under `assets/naruto`. MCreator element definitions are stored in `elements/*.mod.json`; external model sources are in `models/`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from PowerShell:

- `.\gradlew.bat build` compiles Java 21 sources and packages the mod jar.
- `.\gradlew.bat runClient` launches a Minecraft client for manual testing.
- `.\gradlew.bat runServer` launches a dedicated server run.
- `.\gradlew.bat test` runs Gradle tests if test sources are added.

Build output is written to `build/`; local Minecraft run data is written to `run/`. Both are ignored.

## Coding Style & Naming Conventions

Keep Java in package `net.mcreator.naruto`. Follow the existing MCreator style: tabs for indentation in generated Java, PascalCase class names, camelCase methods and fields, and registry classes prefixed with `NarutoMod`. Match existing suffixes such as `Ability`, `Procedure`, `Message`, `Packet`, `Item`, and `Entity`. Resource paths should stay lowercase with underscores, for example `scroll_great_fireball.json` and `jutsu_fireball.png`.

## Testing Guidelines

There is no dedicated test tree currently. For logic-heavy additions, add tests under `src/test/java` and run `.\gradlew.bat test`. For gameplay, always smoke-test in `runClient`; verify item registration, keybinds, packets, cooldowns, particles, sounds, and client/server behavior when relevant. Use `runServer` for changes touching networking, attachments, commands, or server-only logic.

## Commit & Pull Request Guidelines

No commit-message pattern could be verified from local history. Use concise imperative subjects such as `Add chidori cooldown packet` or `Fix scroll texture paths`. Pull requests should describe gameplay impact, list manual test steps, mention affected MCreator elements, and include screenshots or short clips for UI, models, particles, shaders, or visual effects.

## Agent-Specific Instructions

Do not overwrite generated MCreator regions unless the change is intentional. Prefer editing user code blocks and standalone custom classes when possible. Keep `naruto.mcreator`, `elements/`, Java registries, and resources synchronized when adding or renaming content.
