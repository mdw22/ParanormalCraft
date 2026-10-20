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
| `trace start` / `trace stop` | Starts or stops a JSON Lines trace. The client writes `<client game dir>/paranormalcraft-trace/trace-<timestamp>.jsonl` and the server `<server dir>/paranormalcraft-trace/server-trace-<timestamp>.jsonl`. Every line has the game time `t` and a `type`. **Client events:** our sounds as they play (id, position, volume, pitch, `local`), `ghost_render` (alpha, visible, tinted, pickable; on change and every tick of a fade), `figure_spawn` / `figure_sound` / `figure_vanish`, `emf`, `thermometer`, `payload`, `goggles_on` / `goggles_off`. **Server events:** `haunt` / `haunt_none` (action, target, forced, gated), `goal_change`, `manifest_start` / `manifest_end`, `presence`, `hallucination_sent`, `ghost_spawn` / `ghost_disperse`, `tether_start` / `tether_end` (reason) / `tether_drain`, `wand_overheat`, `backfire`, `weakened_start` / `weakened_end`, `trap_armed`, `trap_capture_start` / `trap_capture_success` / `trap_capture_fail`, `trap_release`, `trap_release_hold`, `trap_deploy`, `containment_load`, `containment_queue`, `containment_process_start` / `containment_process_done` (yield), `containment_release`, `containment_charge`, `containment_unstable_start` / `containment_unstable_end`, `ghost_ascended`, `ward_placed`, `ward_ring`, `ward_flare`, `ward_denied_spawn`, `bind_start` / `bind_done` / `bind_cancel`, `unbind`, `familiar_summon` / `familiar_recall` (reason), `wisp_light_place` / `wisp_light_remove` / `wisp_light_sweep`, `wisp_warn`, `wisp_guide` (with `locateMs`) / `wisp_guide_end`, `phase_start` / `phase_end` (reason, extended), `journal_open`, `journal_capture_logged`, `figure_result`. Trace fields must not be called `t` or `type`, which every line already has. The client also traces `beam_render`, `figure_result` / `figure_seen`, `journal_screen` and `ward_preview`. |
| `trace mark <label>` | Writes a label into both traces, to bracket a step. |
| `trace snapshot` | One client line with the whole state: you, goggles, every ghost in range with alpha and visibility, the active figure, EMF and thermometer. |
| `dump [radius]` | Ghosts (with name, Weakened, tethers and who holds them, recovery timer, speed factor), filled traps (with what they hold), Containment Units (with the Vessel slot and bind progress), Ward Lanterns, Wisp Familiars (owner, light block, warning, guiding), the wisp light blocks still listed, and players near you (with any phase), written to chat and `<server dir>/paranormalcraft-debug/`. |

Prefer trace and `dump` over screenshots for anything that is state, timing or audio.

**Fast, repeatable setups**

| Command | What it does |
| --- | --- |
| `scene <name>` | Builds a fixed setup on a floating 48×48 grass platform at (100000, 200, 100000): clears the last scene, sets time and weather, places a cabin if the scene has one, spawns ghosts with a fixed home and cold spot, and puts you (and Dev2 if online) at a fixed spot and view in survival with the kit. Scenes: `arena`, `arena_night`, `cabin_oak_night`, `cabin_spruce_night`, `haunt_props`, `two_player`, `figure_indoor`, `capture_arena` (a ghost at 6 Presence over an armed trap, a second trap 3 blocks east, you 6 blocks south with goggles on and the Tether Wand selected, Dev2 6 blocks west facing it), `containment_lab` (a Containment Unit with a chest of soul sand feeding it through a side hopper, a bottom hopper into a chest, and two Filled Ghost Traps on the platform), `familiar_cave` (midnight, a sealed dark stone tunnel, a zombie waiting round a corner behind you, a Bound Spirit Vessel in your hand). Add new ones in `DebugScenes`. |
| `kit [tools\|none]` | Goggles, EMF Reader, thermometer, a netherite sword, the Tether Wand (slot 5), 3 Ghost Traps, 2 Ward Lanterns, a Field Journal and a Containment Unit in the hotbar; 4 soul sand, an empty Spirit Vessel and a Spectral Step in the inventory; `none` empties the inventory. |
| `tether <players> on\|off\|deploy` | `on`/`off` keeps those players firing their held Tether Wand without holding use (the real server path); a broken tether turns it off, as releasing use would. `deploy` does what attack does while latched: drops a Ghost Trap from their inventory under the ghost. |
| `journal <player> <ghostType> <stage>` / `journal <player> reset\|open` | Sets that player's Field Journal stage (0–4, or 5 for all four plus the bound line) for a ghost type, clears their journal and capture log, or opens their journal. |
| `containment <pos> charge <n>\|fill <n>\|unstable\|process <i>\|advance <ticks>\|release <i>\|bindstart <i>\|unbind` | Sets a Containment Unit's charge, stores fresh Lingering Spirits, empties it to Unstable, queues or releases a stored ghost (as you), moves processing forward, starts a normal 60 s bind into the vessel in its Vessel slot, or unbinds the vessel there. |
| `bind <pos> <i>` | Binds stored ghost `i` into the unit's Vessel slot at once (an empty vessel is put there if the slot is empty). |
| `familiar <player> summon\|recall\|guide` | Summons the Wisp Familiar of the first Bound Spirit Vessel in their inventory, recalls it, or sends it guiding (cooldown skipped). |
| `phase <player> start\|end` | Starts or ends a Spectral Step phase without the charm. |
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
- `crossing_beams` (needs Dev2: crossed beams backfire);
- `steer_to_trap` (tether, then turn: the ghost is steered over a trap and captured);
- `containment_cycle` (a Filled Trap in through the hopper chain, fuel, processing, ectoplasm out the bottom);
- `release_blessing` (release from containment: the ghost ascends, the player is Restful);
- `ward_blocks_haunting` (a Ward Lantern stops a ghost's haunting; without it the ghost haunts again);
- `bind_cycle` (bind a stored spirit, summon its wisp, recall it, put the vessel back and unbind);
- `wisp_warns` (`familiar_cave`: the wisp lights the tunnel and warns of the zombie behind the corner; the warning ends when it's gone, and a recall takes the light);
- `phase_through_door` (a phase that runs out inside a door is extended, then ends once you step out).

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

## Capture and containment tools (Milestone 2)

- **Tether Wand** (amethyst shard over a lightning rod over copper, redstone, copper): hold use to fire a 12-block beam that latches onto the first ghost you can see near the crosshair. It drains 2 Presence a second, breaks past 14 blocks, after 1 s without line of sight (doors, panes, fences and glass don't block it), if you can no longer see the ghost, or on release. 8 s of fire overheats it for 4 s (heat shows on the item bar). Two crossing beams backfire.
  - **Steering:** the ghost is held at a point in front of you (where it was when the beam latched, 4–10 blocks); turn and it follows. Sneak to reel it in, walk backward to let it out. A fresh ghost fights and jerks off the line; a Weakened one is easy to lead.
  - **Attack while latched** drops a Ghost Trap from your inventory (off-hand first, then hotbar, then the rest) under the ghost.
- **Ghost Trap** (iron, tinted glass, iron / copper, redstone, copper / iron, stone pressure plate, iron): right-click throws it, sneak + right-click places it. It pulls in a Weakened ghost (5 Presence or less) floating over it: always if a tether holds it for the whole 1.5 s pull, otherwise half the time.
- **Filled Ghost Trap**: rattles, glows faintly and reads as a ghost on the EMF Reader (up to level 3). Sneak with an empty hand (or break it) to pick it up; the item keeps the ghost, its name, who caught it and where. Hold use on it with an empty hand for 2 s to release the ghost at full Presence, with its old home.
- **Containment Unit** (iron, tinted glass, iron / copper block, Ghost Trap, copper block / iron, redstone block, iron): holds 4 ghosts. Load Filled Traps in the trap slot (the empty trap comes back out), fuel it with soul sand or soul soil (each adds 20 charge; every stored ghost drains 1 per 30 s). At 0 charge with ghosts inside it goes Unstable: an alarm, a red flicker, processing stops. Pick a ghost and:
  - **Process** it over 5 minutes into 3–5 Lingering Ectoplasm (and sometimes a Spectral Residue);
  - **Release** it: it rises and fades, and you get Restful (no phantoms for 3 days).

  Hoppers: fuel from the top or sides, traps from the sides, outputs from the bottom. A comparator reads how many ghosts it holds. Breaking it drops its ghosts as Filled Traps.
- **Ward Lantern** (lantern, 4 Lingering Ectoplasm, amethyst shard): within 32 blocks (config `wardLanternRadius`) no monsters spawn naturally, ghosts don't haunt and nobody sees shadow figures. Ghosts can still be inside, and caught, but they won't choose to wander within 4 blocks of it. Placing one traces its radius in cyan motes; holding one shows the edges of nearby wards. When it stops a haunting it flares bright, chimes, and pushes the ghost back; refused monster spawns leave a small puff of smoke.
- **Spirit Vessel** (copper ingot / glass, Lingering Ectoplasm, glass / three glass): put it in a Containment Unit's Vessel slot, pick a stored ghost and press **Bind** (60 s, 5 charge). The Bound Spirit Vessel carries the ghost; put it back and press **Unbind** to return it to storage.
- **Wisp Familiar** (a bound Lingering Spirit): use the bound vessel to summon or recall it. It follows at your shoulder, carries a light where it's dark, turns red and chirps from the side of a hostile you can't see, and sneak + use sends it toward the nearest Abandoned Cabin (2 minute cooldown). It goes back into the vessel if you drop or store the vessel, change dimension or log out.
- **Spectral Step** (3 Lingering Ectoplasm, phantom membrane, amethyst shard; 20 uses, repair with ectoplasm): use it to walk through doors, trapdoors, fences, gates, walls, bars, panes and glass for 5 s (60 s cooldown). If it runs out while you're inside something, it lasts a little longer, then nudges you out.
- **Field Journal** (book, copper ingot; one in every cabin chest): use it to read the field notes (a diagram of the whole process), a spread per ghost type you've met (a portrait, icon rows of what finds, calms and becomes of it, and short notes that fill in as you detect, see, catch and study it) and a log of your captures. Hover any icon for its name. The stages are advancements in the Paranormal Craft tab.

## Layout

- `common`: all gameplay code, compiled against vanilla Minecraft only
- `neoforge`, `fabric`: thin loader entrypoints and platform implementations

## Built from

This project is built from the [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) by Jared (branch `26.2`).
