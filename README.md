# Playtime + AFK Tracker (Fabric, Minecraft 26.1.2)

Lightweight **server-side** Fabric mod. It shows each player's total playtime in
whole hours next to their name in the tab list (`Steve [12h]`), and after
5 minutes without moving it adds `[AFK]` in the tab list **and** above the
player's head. The `[AFK]` tag clears within ~1 second of moving.

This replaces the
[ChrqnosMC/AFK-Display](https://github.com/ChrqnosMC/AFK-Display) datapack — you
do not need the datapack and this mod, just the mod.

## Requirements

- Minecraft **`26.1.2`** dedicated server (the 26.1 line; `~26.1`)
- **Java 25** (required by Minecraft 26.1)
- Fabric Loader `>= 0.19.2`
- Fabric API `0.149.1+26.1.2`

## Installation

Drop `playtimetracker-1.0.0.jar` together with Fabric API into the server's
`mods/` folder and restart the server.

## Behavior (fixed — no configuration)

| Aspect | Value |
|---|---|
| Playtime format | whole hours, `" [Nh]"` suffix, **tab list only** |
| Playtime refresh | every 60 s (and immediately on join) |
| AFK threshold | 300 s (5 min) with no X/Y/Z change — camera rotation does **not** count |
| AFK display | tab list **and** above the head, cleared ~1 s after moving |
| On going AFK | player joins the managed `ptt-afk` scoreboard team (their previous team is remembered and restored when they return) |
| Commands / config | none — fixed values |

The playtime value is read from the vanilla `minecraft:play_time` statistic
(in ticks), so it reflects the total time the player has been on the server.

## Build

This repository was built with a project-local toolchain (`.tooling/`,
gitignored) because the build environment had no system-wide JDK 25 / Gradle.
With JDK 25 on your `PATH`:

```bash
./gradlew build
```

The mod jar is produced at `build/libs/playtimetracker-1.0.0.jar`. Put that file
into the server's `mods/` folder (together with Fabric API).

> Minecraft 26.1 is the first **unobfuscated** Minecraft version, so this
> project uses the new non-remapping `net.fabricmc.fabric-loom` plugin with no
> `mappings` dependency. The Gradle daemon must run on Java 25.

## Verification status

- **Automated:** 10 JUnit tests cover the pure logic (`PlaytimeFormatter`
  tick→hour flooring boundaries; `AfkTracker` 300 s threshold, position-only
  detection, state-change signal, reset/remove). `./gradlew build` is green.
- **Manual:** verified in-game on a Minecraft `26.1.2` server (tab display,
  `[AFK]` after 5 min in tab and above the head, clearing on movement).

### Manual integration checklist

1. `./gradlew runServer` (JDK 25), or install the jar on a 26.1.2 server with
   Fabric API. On first start set `eula=true` in `run/eula.txt` and restart.
2. Check `logs/latest.log` for `mixin` — `ServerPlayerTabNameMixin` should apply
   without errors.
3. Join the server, open the tab list → the name shows `<name> [Nh]`.
4. Stand completely still for 5 minutes (rotating the camera is allowed and must
   **not** clear AFK) → tab shows `<name> [Nh] [AFK]` and `[AFK]` appears above
   the head.
5. Move one block → within ~1 s `[AFK]` disappears from the tab list and above
   the head; `[Nh]` remains.
6. (Optional) Pre-seed a player's stats JSON (`world/stats/<uuid>.json`,
   `minecraft:play_time` in ticks) to quickly verify the hour rollover.
