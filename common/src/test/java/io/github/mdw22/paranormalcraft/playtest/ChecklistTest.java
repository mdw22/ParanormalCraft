package io.github.mdw22.paranormalcraft.playtest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistTest {

    private static final String GOOD = """
            { "title": "T", "steps": [
              { "id": "one", "title": "One", "lines": ["Do this."], "setup": ["time set noon", "wait 5"], "auto_check": "trace bind_done" },
              { "id": "two", "title": "Two", "lines": ["A", "B", "C"], "auto_check": "expect dump familiars.length=1" },
              { "id": "three", "title": "Three", "lines": ["Look."] } ] }
            """;

    private static List<String> problems(String json) {

        Checklist.InvalidChecklistException e = assertThrows(Checklist.InvalidChecklistException.class,
                () -> Checklist.parse("bad", "data/x/playtests/bad.json", json));
        assertEquals("data/x/playtests/bad.json", e.file());
        assertTrue(e.getMessage().startsWith("data/x/playtests/bad.json: "), e.getMessage());
        return e.problems();
    }

    @Test
    void parsesAGoodFile() throws Exception {

        Checklist checklist = Checklist.parse("good", "good.json", GOOD);
        assertEquals("good", checklist.name());
        assertEquals(3, checklist.steps().size());
        Checklist.Step one = checklist.steps().get(0);
        assertEquals(List.of("time set noon", "wait 5"), one.setup());
        assertNotNull(one.autoCheck());
        assertEquals("bind_done", one.autoCheck().type());
        assertNotNull(checklist.steps().get(1).autoCheck(), "an 'expect ' prefix is allowed");
        assertNull(checklist.steps().get(2).autoCheck());
        assertTrue(checklist.steps().get(2).setup().isEmpty());
    }

    @Test
    void reportsEveryProblemWithItsStep() {

        List<String> problems = problems("""
                { "title": "", "steps": [
                  { "id": "Bad Id", "title": "x", "lines": [] },
                  { "id": "a", "lines": ["1", "2", "3", "4"], "setup": ["wait soon", "expect trace x"] },
                  { "id": "a", "title": "dup", "lines": ["ok"], "auto_check": "maybe later" },
                  { "id": "c", "title": "c", "lines": [3] } ] }
                """);
        String all = String.join("\n", problems);
        assertTrue(all.contains("the checklist: \"title\" must be a non-empty string"), all);
        assertTrue(all.contains("step 1: id 'Bad Id'"), all);
        assertTrue(all.contains("step 1: \"lines\" needs 1 to 3 lines, has 0"), all);
        assertTrue(all.contains("step 2: \"title\" must be a non-empty string"), all);
        assertTrue(all.contains("step 2: \"lines\" needs 1 to 3 lines, has 4"), all);
        assertTrue(all.contains("step 2: bad setup line 'wait soon'"), all);
        assertTrue(all.contains("step 2: setup can't use expect"), all);
        assertTrue(all.contains("step 3: bad auto_check 'maybe later'"), all);
        assertTrue(all.contains("step 4: \"lines\" must be a list of strings"), all);
    }

    @Test
    void duplicateIdsAreAProblem() {

        List<String> problems = problems("""
                { "title": "T", "steps": [ { "id": "a", "title": "A", "lines": ["x"] }, { "id": "a", "title": "B", "lines": ["y"] } ] }
                """);
        assertEquals(List.of("step 2: duplicate id 'a'"), problems);
    }

    @Test
    void notJsonOrNoSteps() {

        assertTrue(problems("{ not json").getFirst().startsWith("not valid JSON"));
        assertEquals(List.of("the top level must be an object"), problems("[]"));
        assertEquals(List.of("\"steps\" must be a non-empty list"), problems("{ \"title\": \"T\", \"steps\": [] }"));
    }

    /** Every shipped checklist parses and validates (setup commands are checked against the real dispatcher in a GameTest). */
    @Test
    void shippedChecklistsAreValid() throws IOException {

        Path dir = Path.of("src/main/resources/data/paranormalcraft/playtests");
        assertTrue(Files.isDirectory(dir), "no checklist folder at " + dir.toAbsolutePath());
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> list = files.filter(path -> path.toString().endsWith(".json")).toList();
            assertFalse(list.isEmpty());
            for (Path file : list) {
                String name = file.getFileName().toString().replace(".json", "");
                try {
                    Checklist.parse(name, file.toString(), Files.readString(file));
                } catch (Checklist.InvalidChecklistException e) {
                    throw new AssertionError(e.getMessage(), e);
                }
            }
        }
    }

    @Test
    void newestPicksTheHighestUpdateAndNeverTheSelfTest() {

        assertEquals("update10", Checklists.newest(List.of("update9", "selftest", "update10", "update2")));
        assertEquals("zeta", Checklists.newest(List.of("alpha", "zeta", "selftest")));
        assertNull(Checklists.newest(List.of("selftest")));
    }
}
