package io.github.mdw22.paranormalcraft.playtest;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The shipped checklists: {@code data/<namespace>/playtests/<name>.json} from the server's data packs, read when
 * asked (so {@code /reload} picks up edits). Invalid files are logged with their problems and left out.
 */
public final class Checklists {

    public static final String FOLDER = "playtests";
    /** Not offered on join: it only exercises the playtest system itself. */
    public static final String SELF_TEST = "selftest";
    private static final Pattern UPDATE = Pattern.compile("update(\\d+)");

    /** Everything found: the valid checklists by name, and the problems of the invalid ones. */
    public record Loaded(Map<String, Checklist> checklists, List<Checklist.InvalidChecklistException> invalid) {
    }

    private Checklists() {
    }

    public static Loaded load(MinecraftServer server) {

        Map<String, Checklist> checklists = new TreeMap<>();
        List<Checklist.InvalidChecklistException> invalid = new ArrayList<>();
        Map<Identifier, Resource> files = server.getResourceManager().listResources(FOLDER, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            String path = file.getKey().getPath();
            String name = path.substring(FOLDER.length() + 1, path.length() - ".json".length());
            try (Reader reader = file.getValue().openAsReader()) {
                StringBuilder text = new StringBuilder();
                char[] buffer = new char[4096];
                for (int read; (read = reader.read(buffer)) > 0; ) {
                    text.append(buffer, 0, read);
                }
                checklists.put(name, Checklist.parse(name, file.getKey().toString(), text.toString()));
            } catch (Checklist.InvalidChecklistException e) {
                Constants.LOG.warn("Invalid playtest checklist {}", e.getMessage());
                invalid.add(e);
            } catch (IOException e) {
                Constants.LOG.warn("Couldn't read playtest checklist {}: {}", file.getKey(), e.getMessage());
                invalid.add(new Checklist.InvalidChecklistException(file.getKey().toString(), List.of("unreadable: " + e.getMessage())));
            }
        }
        return new Loaded(checklists, invalid);
    }

    /**
     * The newest checklist to offer on join: the highest {@code updateN}, otherwise the last by name; never the self
     * test.
     */
    public static @Nullable String newest(Iterable<String> names) {

        List<String> list = new ArrayList<>();
        names.forEach(list::add);
        list.remove(SELF_TEST);
        return list.stream().max(Comparator.comparingInt(Checklists::updateNumber).thenComparing(Comparator.naturalOrder())).orElse(null);
    }

    static int updateNumber(String name) {

        Matcher matcher = UPDATE.matcher(name);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
