# Playtime + AFK Tracker

Shows total playtime in the tab list and marks players AFK after five minutes
without changing block position. Choose the server-only **No GUI** edition or
the **GUI** edition with an inventory-sized player-statistics leaderboard.

## Which edition?

| Edition | Minecraft / loader | Install on | Includes |
|---|---|---|---|
| **No GUI — Forge** | 1.18.2, Forge 40.3.11+, Java 17 | Dedicated server only | Playtime + AFK in tab; AFK above the head; unmodified clients supported by design |
| **GUI — Forge** | 1.18.2, Forge 40.3.11+, Java 17 | Server and every client | Everything above + six-category leaderboard, search and player details |
| **No GUI — Fabric** | 26.1.2, Fabric, Java 25 | Server only | Original playtime + AFK tracker; see the Fabric section below |

**No GUI means no client mod is needed; the server still needs the stated mod
loader. These JARs do not run on an unmodified Mojang Vanilla server.** This
project currently provides no Paper/Spigot plugin or Vanilla datapack edition.
Use the same Minecraft version on the server and clients. Other installed
server mods must also permit unmodified clients for Vanilla clients to join.

The two Forge editions use the same mod ID. Install **one edition at a time**;
remove the previous tracker JAR when switching. Existing vanilla playtime
statistics are preserved.

## Forge 1.18.2 — version 1.2.0

- **No GUI:** `playtimetracker-1.18.2-forge-nogui-1.2.0.jar` — install on the server;
  players join using ordinary 1.18.2 clients without this mod. Contains no custom
  client channel, client classes, inventory button or leaderboard.
- **GUI:** `playtimetracker-1.18.2-forge-gui-1.2.0.jar` — install the matching JAR on
  server and clients. Opens via the FTB Library inventory sidebar, a fallback
  inventory button, `/playtime`, or `/leaderboard`. The 176 × 166 GUI-pixel panel
  uses Minecraft inventory styling with six item tabs and player details.

[Forge installation, behavior and build guide](forge-1.18.2/README.md)

### Download builds

The [Forge GUI and No GUI builds workflow](https://github.com/CodingFlexx/minecraft-playtimetracker/actions/workflows/forge-builds.yml)
builds, tests and verifies both variants on pushes to `main`, relevant pull
requests, and manual runs. Open a successful run and choose the `gui` or `nogui`
artifact. Each contains the runtime JAR, a separate sources JAR and SHA-256 files.
Install the runtime JAR only. Artifacts are kept for 30 days; a GitHub release is
not created automatically.

### Build locally

With Java 17 and Python 3.11+:

```sh
cd forge-1.18.2
./gradlew -Pvariant=nogui build
./gradlew -Pvariant=gui build
```

Outputs are separated into `forge-1.18.2/build/nogui/libs/` and
`forge-1.18.2/build/gui/libs/`. The default variant is `gui`.

Both editions share the tracking code and core tests. Archive checks verify that
No GUI contains no client/leaderboard code and that GUI contains its matching UI,
network classes and translations. Forge multiplayer handshakes and the UI still
need a complete in-game test; automated checks are not a full gameplay test.

## Original Fabric 26.1.2 edition — No GUI

Lightweight **server-side** Fabric mod. It shows each player's total playtime in
whole hours next to their name in the tab list (`Steve [12h]`), and after
5 minutes without moving it adds `[AFK]` in the tab list **and** above the
player's head. The `[AFK]` tag clears within ~1 second of moving.

This replaces the
[ChrqnosMC/AFK-Display](https://github.com/ChrqnosMC/AFK-Display) datapack — you
do not need the datapack and this mod, just the mod.

### Requirements

- Minecraft **`26.1.2`** dedicated server (the 26.1 line; `~26.1`)
- **Java 25** (required by Minecraft 26.1)
- Fabric Loader `>= 0.19.2`
- Fabric API `0.149.1+26.1.2`

### Installation

Drop `playtimetracker-1.0.0.jar` together with Fabric API into the server's
`mods/` folder and restart the server.

### Behavior (fixed — no configuration)

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

### Build

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

### Verification status

- **Automated:** 10 JUnit tests cover the pure logic (`PlaytimeFormatter`
  tick→hour flooring boundaries; `AfkTracker` 300 s threshold, position-only
  detection, state-change signal, reset/remove). `./gradlew build` is green.
- **Manual:** verified in-game on a Minecraft `26.1.2` server (tab display,
  `[AFK]` after 5 min in tab and above the head, clearing on movement).

#### Manual integration checklist

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
