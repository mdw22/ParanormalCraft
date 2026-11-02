package io.github.mdw22.paranormalcraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sentences in the generated {@code en_us} lang start with a capital letter. A value that looks like a sentence: it
 * ends in {@code . ! ?}, or runs to {@link #SENTENCE_WORDS} words (tooltips often have no full stop). Short label
 * fragments ("a job", "%s nights") are fine.
 */
class LangCapitalisationTest {

    private static final Path LANG = Path.of("src/generated/resources/assets/paranormalcraft/lang/en_us.json");
    private static final int SENTENCE_WORDS = 4;
    /** A later sentence in the same value starting in lower case. */
    private static final Pattern LOWER_AFTER_STOP = Pattern.compile("[.!?] +[a-z]");
    /** Keys allowed to break the rule. Keep this short, with a reason each. */
    private static final Set<String> ALLOWED = Set.of();

    static boolean looksLikeSentence(String value) {

        boolean spaced = value.contains(" ");
        return spaced && (value.matches(".*[.!?]$") || value.trim().split(" +").length >= SENTENCE_WORDS);
    }

    static boolean badlyCapitalised(String value) {

        return looksLikeSentence(value) && Character.isLowerCase(value.charAt(0)) || LOWER_AFTER_STOP.matcher(value).find();
    }

    @Test
    void theRuleCatchesWhatItShould() {

        assertTrue(badlyCapitalised("phantoms won't spawn for you for %s nights"));
        assertTrue(badlyCapitalised("the needle jumps."));
        assertTrue(badlyCapitalised("The needle jumps. it won't settle."));
        assertEquals(false, badlyCapitalised("a job"));
        assertEquals(false, badlyCapitalised("%s nights"));
        assertEquals(false, badlyCapitalised("%s has dispersed."));
        assertEquals(false, badlyCapitalised("Bound, it is a Wisp."));
    }

    @Test
    void everySentenceInTheLangStartsWithACapital() throws IOException {

        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        assertTrue(lang.size() > 100, "lang looks empty");
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : lang.entrySet()) {
            if (!ALLOWED.contains(entry.getKey()) && badlyCapitalised(entry.getValue().getAsString())) {
                bad.add(entry.getKey() + " = " + entry.getValue().getAsString());
            }
        }
        assertEquals(List.of(), bad);
    }
}
