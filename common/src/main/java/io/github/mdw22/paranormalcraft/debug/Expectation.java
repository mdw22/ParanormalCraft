package io.github.mdw22.paranormalcraft.debug;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One {@code expect} line of a debug script, parsed, plus the matching rules. Plain logic so it can be unit tested.
 * <ul>
 *     <li>{@code expect trace <type> [field<op>value …] [within <ticks>]}: a trace event of that type whose fields
 *     match, since the script's last command. Waits up to {@code within} ticks (default 20).</li>
 *     <li>{@code expect dump <path><op><value> … [within <ticks>]}: the current debug dump (default: now only).</li>
 * </ul>
 * Operators: {@code = != > < >= <=}; numbers compare as numbers, anything else as text. Paths use dots, array
 * indices and {@code [*]} for "any element", and {@code length}, e.g. {@code ghosts[0].presence},
 * {@code ghosts[*].weakened=true}, {@code ghosts.length=1}.
 */
public record Expectation(Kind kind, @Nullable String type, List<Condition> conditions, int within) {

    public static final int DEFAULT_TRACE_WITHIN = 20;
    private static final String[] OPERATORS = {">=", "<=", "!=", "=", ">", "<"};

    public enum Kind {
        TRACE,
        DUMP
    }

    public record Condition(String path, String operator, String value) {

        @Override
        public String toString() {

            return this.path + this.operator + this.value;
        }
    }

    /**
     * Parses the words after {@code expect}.
     *
     * @throws IllegalArgumentException with a readable reason
     */
    public static Expectation parse(String text) {

        List<String> words = new ArrayList<>(List.of(text.strip().split("\\s+")));
        if (words.isEmpty() || words.getFirst().isEmpty()) {
            throw new IllegalArgumentException("expect needs 'trace' or 'dump'");
        }
        String kindWord = words.removeFirst().toLowerCase(Locale.ROOT);
        Kind kind = switch (kindWord) {
            case "trace" -> Kind.TRACE;
            case "dump" -> Kind.DUMP;
            default -> throw new IllegalArgumentException("expect needs 'trace' or 'dump', not '" + kindWord + "'");
        };
        int within = kind == Kind.TRACE ? DEFAULT_TRACE_WITHIN : 0;
        int withinAt = words.indexOf("within");
        if (withinAt >= 0) {
            if (withinAt != words.size() - 2) {
                throw new IllegalArgumentException("'within <ticks>' must come last");
            }
            try {
                within = Integer.parseInt(words.get(withinAt + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("within needs a whole number of ticks");
            }
            words = new ArrayList<>(words.subList(0, withinAt));
        }
        String type = null;
        if (kind == Kind.TRACE) {
            if (words.isEmpty()) {
                throw new IllegalArgumentException("expect trace needs an event type");
            }
            type = words.removeFirst();
        } else if (words.isEmpty()) {
            throw new IllegalArgumentException("expect dump needs a condition like ghosts.length=1");
        }
        List<Condition> conditions = new ArrayList<>();
        for (String word : words) {
            conditions.add(condition(word));
        }
        return new Expectation(kind, type, List.copyOf(conditions), within);
    }

    private static Condition condition(String word) {

        for (String operator : OPERATORS) {
            int at = word.indexOf(operator);
            if (at > 0) {
                return new Condition(word.substring(0, at), operator, word.substring(at + operator.length()));
            }
        }
        throw new IllegalArgumentException("'" + word + "' isn't field<op>value (ops: = != > < >= <=)");
    }

    /**
     * Whether a trace event matches: right type, every condition true.
     */
    public boolean matchesEvent(JsonObject event) {

        return this.type != null && this.type.equals(stringField(event, "type")) && satisfied(event) == this.conditions.size();
    }

    /**
     * How many conditions hold on {@code root} (for "closest event" reports).
     */
    public int satisfied(JsonElement root) {

        int count = 0;
        for (Condition condition : this.conditions) {
            if (holds(root, condition)) {
                count++;
            }
        }
        return count;
    }

    public boolean matchesDump(JsonObject dump) {

        return satisfied(dump) == this.conditions.size();
    }

    /**
     * Of the events given, the one of the expected type that meets the most conditions (latest on a tie); if none has
     * the type, the latest event of any type. Null if there are none.
     */
    public @Nullable JsonObject closest(List<JsonObject> events) {

        JsonObject best = null;
        int bestScore = -1;
        for (JsonObject event : events) {
            if (this.type != null && this.type.equals(stringField(event, "type"))) {
                int score = satisfied(event);
                if (score >= bestScore) {
                    best = event;
                    bestScore = score;
                }
            }
        }
        if (best == null && !events.isEmpty()) {
            best = events.getLast();
        }
        return best;
    }

    public String describe() {

        StringBuilder text = new StringBuilder(this.kind == Kind.TRACE ? "trace " + this.type : "dump");
        for (Condition condition : this.conditions) {
            text.append(' ').append(condition);
        }
        if (this.within > 0) {
            text.append(" within ").append(this.within);
        }
        return text.toString();
    }

    private static @Nullable String stringField(JsonObject object, String key) {

        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    static boolean holds(JsonElement root, Condition condition) {

        for (JsonElement value : resolve(root, condition.path())) {
            if (compare(value, condition.operator(), condition.value())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every value a path points at ({@code [*]} can give several).
     */
    public static List<JsonElement> resolve(JsonElement root, String path) {

        List<JsonElement> current = List.of(root);
        for (String segment : path.split("\\.")) {
            List<JsonElement> next = new ArrayList<>();
            String name = segment;
            String index = null;
            int bracket = segment.indexOf('[');
            if (bracket >= 0 && segment.endsWith("]")) {
                name = segment.substring(0, bracket);
                index = segment.substring(bracket + 1, segment.length() - 1);
            }
            for (JsonElement element : current) {
                JsonElement child = element;
                if (!name.isEmpty()) {
                    if ((name.equals("length") || name.equals("size")) && element.isJsonArray()) {
                        next.add(new JsonPrimitive(element.getAsJsonArray().size()));
                        continue;
                    }
                    if (!element.isJsonObject() || !element.getAsJsonObject().has(name)) {
                        continue;
                    }
                    child = element.getAsJsonObject().get(name);
                }
                if (index == null) {
                    next.add(child);
                } else if (child.isJsonArray()) {
                    JsonArray array = child.getAsJsonArray();
                    if (index.equals("*")) {
                        array.forEach(next::add);
                    } else {
                        try {
                            int i = Integer.parseInt(index);
                            if (i >= 0 && i < array.size()) {
                                next.add(array.get(i));
                            }
                        } catch (NumberFormatException e) {
                            // Not an index: no match.
                        }
                    }
                }
            }
            current = next;
        }
        return current;
    }

    static boolean compare(JsonElement actual, String operator, String expected) {

        if (actual == null || actual.isJsonNull()) {
            return switch (operator) {
                case "=" -> expected.equals("null");
                case "!=" -> !expected.equals("null");
                default -> false;
            };
        }
        if (!actual.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = actual.getAsJsonPrimitive();
        Double number = primitive.isNumber() ? Double.valueOf(primitive.getAsDouble()) : null;
        Double wanted = parseNumber(expected);
        if (number != null && wanted != null) {
            double difference = number - wanted;
            return switch (operator) {
                case "=" -> Math.abs(difference) < 1.0E-6;
                case "!=" -> Math.abs(difference) >= 1.0E-6;
                case ">" -> difference > 1.0E-6;
                case "<" -> difference < -1.0E-6;
                case ">=" -> difference > -1.0E-6;
                case "<=" -> difference < 1.0E-6;
                default -> false;
            };
        }
        String text = primitive.getAsString();
        return switch (operator) {
            case "=" -> text.equals(expected);
            case "!=" -> !text.equals(expected);
            default -> false;
        };
    }

    private static @Nullable Double parseNumber(String text) {

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
