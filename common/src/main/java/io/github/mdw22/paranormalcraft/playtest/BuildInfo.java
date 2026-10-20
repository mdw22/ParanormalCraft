package io.github.mdw22.paranormalcraft.playtest;

import io.github.mdw22.paranormalcraft.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Which build a playtest ran on, for the results header: in a development run, the git commit of the working copy
 * (read from {@code .git}; plus "with uncommitted changes" when {@code git status} can run and finds some) and the
 * loader. Read once and cached.
 */
public final class BuildInfo {

    private static String cached;

    private BuildInfo() {
    }

    public static synchronized String describe(Path runDirectory) {

        if (cached == null) {
            String loader = Services.PLATFORM.getPlatformName();
            String commit = Services.PLATFORM.isDevelopmentEnvironment() ? headCommit(runDirectory) : null;
            if (commit == null) {
                cached = "unknown (not a development run), " + loader;
            } else {
                String status = git(runDirectory, "status", "--porcelain", "--untracked-files=no");
                String changes = status == null ? " (uncommitted changes unknown: git couldn't run)"
                        : status.isBlank() ? "" : " with uncommitted changes";
                cached = "commit " + commit + changes + ", " + loader;
            }
        }
        return cached;
    }

    /**
     * The checked-out commit (7 characters), read from the {@code .git} folder above {@code start}: no git process
     * needed, which a game JVM may not be able to start.
     */
    static String headCommit(Path start) {

        for (Path dir = start.toAbsolutePath().normalize(); dir != null; dir = dir.getParent()) {
            Path git = dir.resolve(".git");
            if (!Files.isDirectory(git)) {
                continue;
            }
            try {
                String head = Files.readString(git.resolve("HEAD"), StandardCharsets.UTF_8).strip();
                if (!head.startsWith("ref: ")) {
                    return shorten(head);
                }
                String ref = head.substring(5).strip();
                Path loose = git.resolve(ref);
                if (Files.isRegularFile(loose)) {
                    return shorten(Files.readString(loose, StandardCharsets.UTF_8).strip());
                }
                Path packed = git.resolve("packed-refs");
                if (Files.isRegularFile(packed)) {
                    for (String line : Files.readAllLines(packed, StandardCharsets.UTF_8)) {
                        if (line.endsWith(" " + ref)) {
                            return shorten(line.substring(0, line.indexOf(' ')));
                        }
                    }
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }
        return null;
    }

    private static String shorten(String hash) {

        return hash.length() > 7 ? hash.substring(0, 7) : hash;
    }

    /** From {@code CommonEvents.onServerStopped}: the next server (another run in the same JVM) reads it again. */
    public static synchronized void clear() {

        cached = null;
    }

    private static String git(Path directory, String... args) {

        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            return process.exitValue() == 0 ? out : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
