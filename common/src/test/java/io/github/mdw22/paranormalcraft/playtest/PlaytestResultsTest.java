package io.github.mdw22.paranormalcraft.playtest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytestResultsTest {

    @Test
    void durationIsMinutesAndSeconds() {

        assertEquals("0:00", PlaytestResults.duration(-5));
        assertEquals("0:09", PlaytestResults.duration(9_999));
        assertEquals("2:05", PlaytestResults.duration(125_000));
    }

    @Test
    void markdownHasAHeaderAndARowPerStep() {

        PlaytestResults.StepResult pass = new PlaytestResults.StepResult("bind", "Bind a ghost", true);
        pass.setStatus(PlaytestResults.Status.PASS);
        pass.autoCheckPassed();
        pass.addMillis(102_000);
        PlaytestResults.StepResult fail = new PlaytestResults.StepResult("wisp_light", "Wisp | light", false);
        fail.setStatus(PlaytestResults.Status.FAIL);
        fail.addNote("light flickers\nat corners");
        fail.addNote("too dim");
        fail.addMillis(30_000);
        PlaytestResults.StepResult skip = new PlaytestResults.StepResult("guide", "Guide", true);
        skip.setStatus(PlaytestResults.Status.SKIP);
        PlaytestResults.StepResult pending = new PlaytestResults.StepResult("journal", "Journal", false);
        String markdown = PlaytestResults.markdown(new PlaytestResults.Header("update10", "Update 10 re-test", "2026-10-19 21:00",
                "commit abc1234, NeoForge", "Dev", "finished"), List.of(pass, fail, skip, pending));
        String expected = """
                # Playtest: update10 — Update 10 re-test

                - **Date:** 2026-10-19 21:00
                - **Build:** commit abc1234, NeoForge
                - **Player:** Dev
                - **Run:** finished
                - **Result:** 1 pass, 1 fail, 1 skip, 1 not reached (4 steps)

                | # | Step | Result | Auto-check | Time | Notes |
                | --- | --- | --- | --- | --- | --- |
                | 1 | Bind a ghost (`bind`) | pass | ✓ passed | 1:42 |  |
                | 2 | Wisp \\| light (`wisp_light`) | **FAIL** | — | 0:30 | light flickers at corners / too dim |
                | 3 | Guide (`guide`) | skip | not met | 0:00 |  |
                | 4 | Journal (`journal`) | not reached | — | 0:00 |  |
                """;
        assertEquals(expected, markdown);
    }

    @Test
    void blankNotesAreIgnored() {

        PlaytestResults.StepResult step = new PlaytestResults.StepResult("a", "A", false);
        step.addNote("   ");
        assertTrue(step.notes().isEmpty());
    }
}
