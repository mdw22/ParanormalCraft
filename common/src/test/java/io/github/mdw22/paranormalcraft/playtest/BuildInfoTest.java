package io.github.mdw22.paranormalcraft.playtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BuildInfoTest {

    @Test
    void readsTheBranchHeadFromALooseRefAboveTheRunFolder(@TempDir Path repo) throws IOException {

        Files.createDirectories(repo.resolve(".git/refs/heads"));
        Files.writeString(repo.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(repo.resolve(".git/refs/heads/main"), "39045d295ed5091923c992b8ea53f9563c0f9c46\n");
        Path runDir = Files.createDirectories(repo.resolve("neoforge/runs/server"));
        assertEquals("39045d2", BuildInfo.headCommit(runDir));
    }

    @Test
    void readsPackedRefsAndDetachedHeads(@TempDir Path repo) throws IOException {

        Files.createDirectories(repo.resolve(".git"));
        Files.writeString(repo.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(repo.resolve(".git/packed-refs"), "# pack-refs with: peeled\nabcdef0123456789 refs/heads/main\n");
        assertEquals("abcdef0", BuildInfo.headCommit(repo));
        Files.writeString(repo.resolve(".git/HEAD"), "1234567890abcdef\n");
        assertEquals("1234567", BuildInfo.headCommit(repo));
    }

    @Test
    void nullOutsideARepository(@TempDir Path dir) {

        assertNull(BuildInfo.headCommit(dir));
    }
}
