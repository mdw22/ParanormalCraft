package io.github.mdw22.paranormalcraft.entity.ghost;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.util.RandomSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generated ghost names ("Ada Crane"): a first and a last name from {@code data/paranormalcraft/ghost_names/names.json}
 * in the mod's resources, read once. Names aren't translated, so the file is the same for every language.
 */
public final class GhostNames {

    private static final String RESOURCE = "/data/" + Constants.MOD_ID + "/ghost_names/names.json";
    private static List<String> first;
    private static List<String> last;

    private GhostNames() {
    }

    public static String random(RandomSource random) {

        load();
        return first.get(random.nextInt(first.size())) + " " + last.get(random.nextInt(last.size()));
    }

    public static List<String> firstNames() {

        load();
        return first;
    }

    public static List<String> lastNames() {

        load();
        return last;
    }

    private static synchronized void load() {

        if (first != null) {
            return;
        }
        List<String> firstNames = new ArrayList<>();
        List<String> lastNames = new ArrayList<>();
        try (InputStream stream = GhostNames.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + RESOURCE);
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            add(json.getAsJsonArray("first"), firstNames);
            add(json.getAsJsonArray("last"), lastNames);
        } catch (Exception e) {
            Constants.LOG.error("Could not read ghost names from {}; using a placeholder", RESOURCE, e);
        }
        if (firstNames.isEmpty() || lastNames.isEmpty()) {
            firstNames = List.of("Nameless");
            lastNames = List.of("Spirit");
        }
        first = List.copyOf(firstNames);
        last = List.copyOf(lastNames);
    }

    private static void add(JsonArray array, List<String> into) {

        array.forEach(element -> into.add(element.getAsString()));
    }
}
