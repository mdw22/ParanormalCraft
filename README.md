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

Dev servers accept the EULA automatically and run with `online-mode=false`, so a dev client can join at `localhost`.

## Layout

- `common`: all gameplay code, compiled against vanilla Minecraft only
- `neoforge`, `fabric`: thin loader entrypoints and platform implementations

## Built from

This project is built from the [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) by Jared (branch `26.2`).
