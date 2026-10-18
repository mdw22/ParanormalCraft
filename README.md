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

Deletes logs, crash reports and `paranormalcraft-debug/` dumps from every folder under `neoforge/runs/` and `fabric/runs/`. It also deletes test worlds: every single-player world in a client's `saves/`, the GameTest server's world, and every server world except the one named by `level-name` in that server's `server.properties`. Config files, `options.txt` and the kept server world stay. Copy any single-player world you want to keep somewhere else first. Fabric GameTest runs live under `fabric/build/run/`, which `./gradlew clean` removes.

Dev servers accept the EULA automatically and run with `online-mode=false`, so a dev client can join at `localhost`.

## Layout

- `common`: all gameplay code, compiled against vanilla Minecraft only
- `neoforge`, `fabric`: thin loader entrypoints and platform implementations

## Built from

This project is built from the [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) by Jared (branch `26.2`).
