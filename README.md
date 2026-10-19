# Paranormal Craft

Hunt, capture and contain the paranormal. A Minecraft Java 26.2 mod for NeoForge and Fabric.

## Requirements

- JDK 25 (Gradle pins the toolchain to 25 and downloads one if none is installed)
- VS Code with the Extension Pack for Java and Gradle for Java (recommended in `.vscode/extensions.json`)

## Build

```sh
./gradlew build
```

Jars are written to `neoforge/build/libs/` and `fabric/build/libs/`.

## Run

| What | Command | VS Code task |
| --- | --- | --- |
| NeoForge client | `./gradlew :neoforge:runClient` | NeoForge client |
| NeoForge server | `./gradlew :neoforge:runServer` | NeoForge server |
| Fabric client | `./gradlew :fabric:runClient` | Fabric client |
| Fabric server | `./gradlew :fabric:runServer` | Fabric server |
| NeoForge data generation | `./gradlew :neoforge:runData` | NeoForge data gen |

In VS Code, run these from **Terminal > Run Task**. To debug the NeoForge client, open the Run and Debug view and start **Debug NeoForge client**.

### Cleaning up the dev run folders

```sh
./gradlew cleanDevRuns
```

Deletes logs, crash reports, `paranormalcraft-debug/` dumps and debug traces and shots (`paranormalcraft-trace/`, `paranormalcraft-shots/`) from every folder under `neoforge/runs/` and `fabric/runs/`. It also deletes test worlds: every single-player world in a client's `saves/`, the GameTest server's world, and every server world except the one named by `level-name` in that server's `server.properties`. Config files, `options.txt` and the kept server world stay. Copy any single-player world you want to keep somewhere else first. Fabric GameTest runs live under `fabric/build/run/`, which `./gradlew clean` removes.

Dev servers accept the EULA automatically and run with `online-mode=false`, so a dev client can join at `localhost`.

## How to test

Check things in this order, and stop at the first step that answers the question:
1. unit tests;
2. server GameTests;
3. client GameTests;
4. trace, dump and scripted runs in a dev world;
5. a screenshot, only when the look itself is the deliverable;
6. a person, for feel: audio, pacing, scariness, art.

### Automated tests

| What | Command |
| --- | --- |
| Unit tests | `./gradlew :common:test` |
| Server GameTests, NeoForge | `./gradlew :neoforge:runGameTestServer` |
| Server GameTests, Fabric | `./gradlew :fabric:runGameTest` (also part of `./gradlew build`) |
| Client GameTests, Fabric | `./gradlew :fabric:runClientGameTest` (screenshots, traces and debug shots in `fabric/build/run/clientGameTest/`) |

### Debug toolkit (`/paranormalcraft debug …`)

These need op level 2 and are on in development runs; elsewhere, set `"enableDebugCommands": true` in `config/paranormalcraft-server.json`. Run them in chat, from the server console, or over RCON with `execute as <player> run …` when a command has to act on a player.

**Capture and tracing**

| Command | What it does |
| --- | --- |
| `screenshot <name> [width height] [player]` | The target client (default: you) grabs its own frame with vanilla's screenshot code and saves `<client game dir>/paranormalcraft-shots/<name>.png`, 854×480 unless you give a size. The path comes back in chat and in both logs. |
| `trace start` / `trace stop` | Starts or stops a JSON Lines trace. The client writes `<client game dir>/paranormalcraft-trace/trace-<timestamp>.jsonl` and the server `<server dir>/paranormalcraft-trace/server-trace-<timestamp>.jsonl`. Every line has the game time `t` and a `type`. **Client events:** our sounds as they play (id, position, volume, pitch, `local`), `ghost_render` (alpha, visible, tinted, pickable; on change and every tick of a fade), `figure_spawn` / `figure_sound` / `figure_vanish`, `emf`, `thermometer`, `payload`, `goggles_on` / `goggles_off`. **Server events:** `haunt` / `haunt_none` (action, target, forced, gated), `goal_change`, `manifest_start` / `manifest_end`, `presence`, `hallucination_sent`, `ghost_spawn` / `ghost_disperse`, `tether_start` / `tether_end` (reason) / `tether_drain`, `wand_overheat`, `backfire`, `weakened_start` / `weakened_end`, `trap_armed`, `trap_capture_start` / `trap_capture_success` / `trap_capture_fail`, `trap_release`, `figure_result`. The client also traces `beam_render` and `figure_result` / `figure_seen`. |
| `trace mark <label>` | Writes a label into both traces, to bracket a step. |
| `trace snapshot` | One client line with the whole state: you, goggles, every ghost in range with alpha and visibility, the active figure, EMF and thermometer. |
| `dump [radius]` | Ghosts (with name, Weakened, tethers and who holds them, recovery timer, speed factor), filled traps (with what they hold) and players near you, written to chat and `<server dir>/paranormalcraft-debug/`. |

Prefer trace and `dump` over screenshots for anything that is state, timing or audio.

**Fast, repeatable setups**

| Command | What it does |
| --- | --- |
| `scene <name>` | Builds a fixed setup on a floating 48×48 grass platform at (100000, 200, 100000): clears the last scene, sets time and weather, places a cabin if the scene has one, spawns ghosts with a fixed home and cold spot, and puts you (and Dev2 if online) at a fixed spot and view in survival with the kit. Scenes: `arena`, `arena_night`, `cabin_oak_night`, `cabin_spruce_night`, `haunt_props`, `two_player`, `figure_indoor`, `capture_arena` (a ghost at 6 Presence over an armed trap, a second trap 3 blocks east, you 6 blocks south with goggles on and the Tether Wand selected, Dev2 6 blocks west facing it). Add new ones in `DebugScenes`. |
| `kit [tools\|none]` | Goggles, EMF Reader, thermometer, a netherite sword, the Tether Wand (slot 5) and 3 Ghost Traps in the hotbar; `none` empties the inventory. |
| `tether <players> on\|off` | Keeps those players firing their held Tether Wand without holding use (the real server path). A broken tether turns it off, as releasing use would. |
| `look <x y z>` / `look <entity>` / `look <yaw> <pitch>` | Turns you to face a point or entity exactly, or sets the angles. |

**Ghost control**

| Command | What it does |
| --- | --- |
| `ghost spawn [x y z]` | A Lingering Spirit homed at that point (default: where you are). |
| `ghost clear [radius]` | Removes ghosts in range (default 64) without dispersal effects. |
| `ghost freeze <targets> on\|off` | AI off and position held, for framing a shot. |
| `ghost goal <targets> wander\|curious\|shy\|home` | Forces that goal for 10 s; the others wait. |
| `manifest <targets>`, `presence <targets> <value>`, `haunt <targets> [action]`, `hallucinate <player>`, `coldspot <targets>`, `savestructure <pos>` | As before. |

**Timers and pace**

| Command | What it does |
| --- | --- |
| `timers <targets> haunt\|manifest <ticks>` | Sets that ghost timer. |
| `timers <player> hallucination <ticks>` | Sets a player's next hallucination attempt. |
| `pace fast\|normal` | Haunting, manifest and hallucination intervals 10× shorter, timers already running included. In memory only; a server restart resets it. Use this instead of editing the config file. |

**Config without restarts**

| Command | What it does |
| --- | --- |
| `config get <key>` | A value; section keys use a dot, e.g. `manifestIntervalSeconds.min`. |
| `config set <key> <value>` | Sets it in memory (clamped and checked like the file), never saved. Client keys (`hallucinationIntensity`, `reduceFlashing`, `temperatureUnit`) are sent to every connected client. |
| `config reload` | Re-reads the server file and asks clients to re-read theirs. |

**Scripted runs**

`run <file>` runs `<server dir>/paranormalcraft-scripts/<file>.txt` line by line on the server tick, as whoever started it:
- each line is a command without the leading `/`;
- `wait <ticks>` waits that many game ticks (so it follows `/tick freeze`, `step` and `sprint`);
- `#` lines are comments.

- `expect trace <type> [field<op>value …] [within <ticks>]` waits up to `within` ticks (default 20) for a matching event since the script's last command, in the server trace and the starting player's client trace (both are captured in memory while a script runs, no `trace start` needed);
- `expect dump <path><op><value> …` checks the `dump` JSON around the starting player now, e.g. `ghosts[0].weakened=true`, `filledTraps.length>=1`, `ghosts[*].manifesting=true`. Operators: `= != < <= > >=`.

The script stops at the first line that fails (a command that fails, or an expectation not met) and reports the line, the expectation and the closest event it saw.

Every line keeps the position the script was started from, even after a scene teleports you, so name coordinates explicitly after a scene. The examples in `tools/scripts/` are copied into the dev server's scripts folder by `runServer`:
- `smoke_ghost` (arena, spawn, manifest, screenshot, disperse);
- `haunt_all` (`haunt_props` and every haunting action);
- `figure_indoor`;
- `capture_solo` (tether, Weakened, a trap under the ghost, captured);
- `capture_coop` (needs Dev2: two tethers, double drain, half speed, captured, no backfire);
- `crossing_beams` (needs Dev2: crossed beams backfire).

### Vanilla tools that help

- **`/tick freeze`, `/tick step <n>`, `/tick sprint <ticks>`, `/tick rate <n>`:** hold a frame, or skip waits.
- **Data changes without a restart:** edit the JSON under `common/src/main/resources`, or re-run `./gradlew :neoforge:runData` for generated files. Then run `./gradlew :neoforge:processResources` (about 7 s; `:fabric:processResources` for a Fabric run) and `/reload` in game.
  - This covers loot tables, tags, recipes, advancements and worldgen data, and structure templates for new `/place` calls.
  - Chunks that already generated keep what they have.
  - The dev server reads `neoforge/build/resources/main`, which is why `processResources` is the step that matters.
- **Code changes without a restart (hot-swap), from VS Code:**
  1. Start **Debug NeoForge client** or **Debug NeoForge server** from Run and Debug. These use `./gradlew :neoforge:runClient -PdebugClient` (port 5005) and `./gradlew :neoforge:runServer -PdebugServer` (port 5006); the game waits for the debugger.
  2. Edit a method body and save. VS Code compiles it and applies it to the running game (Hot Code Replace).
- **Hot-swap from the command line:**
  1. `jdb -attach 127.0.0.1:5006` (or 5005), then `cont` to let the game start.
  2. After a change, `./gradlew :neoforge:compileJava` (about 7 s).
  3. In jdb: `redefine <class> <path to its .class file>`. The class is under `neoforge/build/classes/java/main`; copy it to a path without spaces first, because jdb splits on them.
- **What hot-swap can't do, so these need a full restart:** new or removed methods (including adding or removing a lambda), new fields, changed signatures, new classes that existing code must reach, registry entries (items, blocks, sounds, entities), mixins, and anything read only at startup (configs, payload registration).

## Capture tools (Milestone 2, part A)

- **Tether Wand** (amethyst shard over a lightning rod over copper, redstone, copper): hold use to fire a 12-block beam that latches onto the first ghost you can see near the crosshair. It drains 2 Presence a second, breaks past 14 blocks, after 1 s without line of sight (doors, panes, fences and glass don't block it), if you can no longer see the ghost, or on release. 8 s of fire overheats it for 4 s (heat shows on the item bar). Two crossing beams backfire.
- **Ghost Trap** (iron, tinted glass, iron / copper, redstone, copper / iron, stone pressure plate, iron): right-click throws it, sneak + right-click places it. It pulls in a Weakened ghost (5 Presence or less) floating over it: always if a tether holds it for the whole 1.5 s pull, otherwise half the time.
- **Filled Ghost Trap**: rattles, glows faintly and reads as a ghost on the EMF Reader (up to level 3). Sneak with an empty hand (or break it) to pick it up; the item keeps the ghost, its name, who caught it and where. Placed, sneak + use with an item in hand releases the ghost at full Presence, with its old home.

## Layout

- `common`: all gameplay code, compiled against vanilla Minecraft only
- `neoforge`, `fabric`: thin loader entrypoints and platform implementations

## Built from

This project is built from the [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) by Jared (branch `26.2`).
