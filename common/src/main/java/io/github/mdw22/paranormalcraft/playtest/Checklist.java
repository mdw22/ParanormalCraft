package io.github.mdw22.paranormalcraft.playtest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.mdw22.paranormalcraft.debug.Expectation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A playtest checklist, from {@code data/paranormalcraft/playtests/<name>.json}. Plain logic (Gson only) so parsing
 * and validation are unit tested.
 * <pre>
 * { "title": "Update 10 re-test",
 *   "steps": [ { "id": "bind", "title": "Bind a ghost",
 *                "lines": ["What to do.", "What to look for."],
 *                "setup": ["paranormalcraft debug scene containment_lab", "wait 20"],
 *                "auto_check": "trace bind_start" } ] }
 * </pre>
 * {@code lines}: 1 to {@link #MAX_LINES}. {@code setup} (optional): commands run as the player through the script
 * runner, {@code wait <ticks>} included. {@code auto_check} (optional): an {@code expect} condition without the word
 * {@code expect}, e.g. {@code trace familiar_summon} or {@code dump familiars.length=1}.
 */
public record Checklist(String name, String title, List<Step> steps) {

    public static final int MAX_LINES = 3;

    /**
     * One step. {@code autoCheck} is the parsed condition, or null.
     */
    public record Step(String id, String title, List<String> lines, List<String> setup, @Nullable String autoCheckText,
                       @Nullable Expectation autoCheck) {
    }

    /** A file that failed validation: which file and every problem found. */
    public static final class InvalidChecklistException extends Exception {

        private final String file;
        private final List<String> problems;

        public InvalidChecklistException(String file, List<String> problems) {

            super(file + ": " + String.join("; ", problems));
            this.file = file;
            this.problems = List.copyOf(problems);
        }

        public String file() {

            return this.file;
        }

        public List<String> problems() {

            return this.problems;
        }
    }

    /**
     * Parses and validates a checklist file's text.
     *
     * @param name the checklist's name (the file name without {@code .json})
     * @param file the file, for messages
     */
    public static Checklist parse(String name, String file, String json) throws InvalidChecklistException {

        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new InvalidChecklistException(file, List.of("not valid JSON (" + e.getMessage() + ")"));
        }
        if (!root.isJsonObject()) {
            throw new InvalidChecklistException(file, List.of("the top level must be an object"));
        }
        JsonObject object = root.getAsJsonObject();
        String title = string(object, "title", "the checklist", problems);
        List<Step> steps = new ArrayList<>();
        if (!object.has("steps") || !object.get("steps").isJsonArray() || object.getAsJsonArray("steps").isEmpty()) {
            problems.add("\"steps\" must be a non-empty list");
        } else {
            Set<String> ids = new HashSet<>();
            JsonArray array = object.getAsJsonArray("steps");
            for (int i = 0; i < array.size(); i++) {
                String where = "step " + (i + 1);
                if (!array.get(i).isJsonObject()) {
                    problems.add(where + " must be an object");
                    continue;
                }
                Step step = step(array.get(i).getAsJsonObject(), where, problems);
                if (step != null) {
                    if (!ids.add(step.id())) {
                        problems.add(where + ": duplicate id '" + step.id() + "'");
                    }
                    steps.add(step);
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new InvalidChecklistException(file, problems);
        }
        return new Checklist(name, title, List.copyOf(steps));
    }

    private static @Nullable Step step(JsonObject object, String where, List<String> problems) {

        int before = problems.size();
        String id = string(object, "id", where, problems);
        if (id != null && !id.matches("[a-z0-9_]+")) {
            problems.add(where + ": id '" + id + "' must be lower case letters, digits and _");
        }
        String title = string(object, "title", where, problems);
        List<String> lines = strings(object, "lines", where, true, problems);
        if (lines != null && (lines.isEmpty() || lines.size() > MAX_LINES)) {
            problems.add(where + ": \"lines\" needs 1 to " + MAX_LINES + " lines, has " + lines.size());
        }
        List<String> setup = object.has("setup") ? strings(object, "setup", where, false, problems) : List.of();
        if (setup != null) {
            for (String line : setup) {
                String lower = line.strip().toLowerCase(Locale.ROOT);
                if (lower.startsWith("wait ") && !lower.substring(5).strip().matches("\\d+")) {
                    problems.add(where + ": bad setup line '" + line + "' (wait needs whole ticks)");
                }
                if (lower.startsWith("expect ")) {
                    problems.add(where + ": setup can't use expect; put it in \"auto_check\"");
                }
            }
        }
        String autoCheckText = null;
        Expectation autoCheck = null;
        if (object.has("auto_check")) {
            autoCheckText = string(object, "auto_check", where, problems);
            if (autoCheckText != null) {
                String text = autoCheckText.strip();
                if (text.toLowerCase(Locale.ROOT).startsWith("expect ")) {
                    text = text.substring(7);
                }
                try {
                    autoCheck = Expectation.parse(text);
                } catch (IllegalArgumentException e) {
                    problems.add(where + ": bad auto_check '" + autoCheckText + "': " + e.getMessage());
                }
            }
        }
        if (problems.size() > before) {
            return null;
        }
        return new Step(id, title, List.copyOf(lines), List.copyOf(setup), autoCheckText, autoCheck);
    }

    private static @Nullable String string(JsonObject object, String key, String where, List<String> problems) {

        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString() || element.getAsString().isBlank()) {
            problems.add(where + ": \"" + key + "\" must be a non-empty string");
            return null;
        }
        return element.getAsString();
    }

    private static @Nullable List<String> strings(JsonObject object, String key, String where, boolean required, List<String> problems) {

        JsonElement element = object.get(key);
        if (element == null) {
            if (required) {
                problems.add(where + ": \"" + key + "\" is missing");
            }
            return required ? null : List.of();
        }
        if (!element.isJsonArray()) {
            problems.add(where + ": \"" + key + "\" must be a list of strings");
            return null;
        }
        List<String> list = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                problems.add(where + ": \"" + key + "\" must be a list of strings");
                return null;
            }
            list.add(item.getAsString());
        }
        return list;
    }
}
