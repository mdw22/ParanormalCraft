package io.github.mdw22.paranormalcraft.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.block.FilledGhostTrapBlockEntity;
import io.github.mdw22.paranormalcraft.capture.CapturedGhost;
import io.github.mdw22.paranormalcraft.capture.FilledTraps;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.haunting.HauntingAction;
import io.github.mdw22.paranormalcraft.haunting.HauntingController;
import io.github.mdw22.paranormalcraft.haunting.HauntingSchedule;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import io.github.mdw22.paranormalcraft.tool.ThermometerReadings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * {@code /paranormalcraft debug dump}: ghost and player state as chat lines, log lines and a JSON file in
 * {@code <game dir>/paranormalcraft-debug/}.
 */
public final class DebugDump {

    public static final String FOLDER = "paranormalcraft-debug";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private DebugDump() {
    }

    /**
     * @param chat receives each human-readable line
     * @return the JSON file written, or null if writing failed
     */
    public static @Nullable Path dump(ServerLevel level, Vec3 centre, double radius, Consumer<Component> chat) {

        JsonObject root = build(level, centre, radius, chat);
        Path folder = level.getServer().getServerDirectory().resolve(FOLDER);
        Path file = folder.resolve("dump-" + LocalDateTime.now().format(STAMP) + ".json");
        try {
            Files.createDirectories(folder);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOG.error("Could not write debug dump {}", file, e);
            return null;
        }
        line(chat, "%d ghost(s), %d player(s); wrote %s", root.getAsJsonArray("ghosts").size(), root.getAsJsonArray("players").size(), file.toAbsolutePath());
        return file;
    }

    /**
     * The dump as JSON, without writing a file (script {@code expect dump} uses this).
     *
     * @param chat receives each human-readable line; pass {@code line -> {}} for none
     */
    public static JsonObject build(ServerLevel level, Vec3 centre, double radius, Consumer<Component> chat) {

        List<GhostEntity> ghosts = level.getEntitiesOfClass(GhostEntity.class, new AABB(centre, centre).inflate(radius),
                ghost -> ghost.position().distanceTo(centre) <= radius);
        JsonObject root = new JsonObject();
        root.addProperty("gameTime", level.getGameTime());
        root.addProperty("dimension", level.dimension().identifier().toString());
        root.addProperty("radius", radius);
        root.add("centre", vec(centre));

        JsonArray ghostArray = new JsonArray();
        for (GhostEntity ghost : ghosts) {
            JsonObject json = new JsonObject();
            json.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(ghost.getType()).toString());
            json.addProperty("uuid", ghost.getUUID().toString());
            json.addProperty("name", ghost.ghostName());
            json.add("position", vec(ghost.position()));
            json.add("home", pos(ghost.getHome()));
            json.add("coldSpot", pos(ghost.getColdSpot()));
            json.addProperty("presence", ghost.getPresence());
            json.addProperty("manifesting", ghost.isManifesting());
            json.addProperty("weakened", ghost.isWeakened());
            json.addProperty("tethers", ghost.tetherCount());
            JsonArray holders = new JsonArray();
            TetherManager.holdersOf(ghost).forEach(holders::add);
            json.add("tetheredBy", holders);
            json.addProperty("recoveryTicksLeft", ghost.recoveryTicksLeft());
            json.addProperty("speedFactor", ghost.speedFactor());
            json.addProperty("beingCaptured", ghost.capturingTrap() != null);
            json.addProperty("activeGoal", ghost.activeGoals());
            HauntingSchedule schedule = ghost.haunting().schedule();
            JsonObject haunting = new JsonObject();
            haunting.addProperty("nextAttemptGameTime", schedule.isScheduled() ? schedule.nextAttempt() : null);
            haunting.addProperty("ticksUntilNextAttempt", schedule.isScheduled() ? Math.max(0, schedule.nextAttempt() - level.getGameTime()) : null);
            JsonObject cooldowns = new JsonObject();
            for (HauntingAction action : HauntingAction.values()) {
                cooldowns.addProperty(action.id(), schedule.cooldownRemaining(action, level.getGameTime()));
            }
            haunting.add("cooldownTicksRemaining", cooldowns);
            JsonArray recent = new JsonArray();
            for (HauntingController.Record record : ghost.haunting().history()) {
                recent.add(record.action().id() + " @" + record.gameTime() + ": " + record.detail());
            }
            haunting.add("recent", recent);
            json.add("haunting", haunting);
            ghostArray.add(json);
            line(chat, "Ghost %s \"%s\" %s at %s home %s cold spot %s presence %.1f weakened %s tethers %d manifesting %s goal %s next haunt in %s ticks",
                    json.get("type").getAsString(), ghost.ghostName(), ghost.getUUID(), fmt(ghost.position()), ghost.getHome(), ghost.getColdSpot(),
                    ghost.getPresence(), ghost.isWeakened(), ghost.tetherCount(), ghost.isManifesting(), ghost.activeGoals(),
                    schedule.isScheduled() ? Math.max(0, schedule.nextAttempt() - level.getGameTime()) : "?");
        }
        root.add("ghosts", ghostArray);

        JsonArray playerArray = new JsonArray();
        for (ServerPlayer player : level.players()) {
            JsonObject json = new JsonObject();
            json.addProperty("name", player.getGameProfile().name());
            json.add("position", vec(player.position()));
            JsonObject canSee = new JsonObject();
            StringBuilder seen = new StringBuilder();
            for (GhostEntity ghost : ghosts) {
                boolean sees = GhostVisibility.canSee(player, ghost);
                canSee.addProperty(ghost.getUUID().toString(), sees);
                seen.append(seen.isEmpty() ? "" : ", ").append(ghost.getUUID().toString(), 0, 8).append('=').append(sees);
            }
            int emf = ToolTracker.emfLevel(player);
            double celsius = ThermometerReadings.readWithoutNoise(level, player.position());
            json.add("canSee", canSee);
            json.addProperty("emfLevel", emf);
            json.addProperty("thermometerCelsius", Math.round(celsius * 10.0) / 10.0);
            playerArray.add(json);
            line(chat, "Player %s at %s canSee [%s] EMF %d thermometer %.1f°C (no noise)",
                    json.get("name").getAsString(), fmt(player.position()), seen, emf, celsius);
        }
        root.add("players", playerArray);

        JsonArray trapArray = new JsonArray();
        for (BlockPos trapPos : FilledTraps.near(level, centre, radius)) {
            if (!(level.getBlockEntity(trapPos) instanceof FilledGhostTrapBlockEntity trap)) {
                continue;
            }
            JsonObject json = new JsonObject();
            json.add("pos", pos(trapPos));
            CapturedGhost contents = trap.contents();
            json.addProperty("name", contents == null ? "none" : contents.name());
            json.addProperty("entityType", contents == null ? "none" : contents.entityType());
            json.addProperty("captor", contents == null ? "none" : contents.captor());
            json.addProperty("structure", contents == null ? "none" : contents.structure().orElse("none"));
            json.addProperty("uuid", contents == null || !contents.entity().contains("UUID") ? "none"
                    : UUIDUtil.uuidFromIntArray(contents.entity().getIntArray("UUID").orElse(new int[4])).toString());
            trapArray.add(json);
            line(chat, "Filled trap at %s holds %s (caught by %s)", trapPos.toShortString(), json.get("name").getAsString(), json.get("captor").getAsString());
        }
        root.add("filledTraps", trapArray);
        return root;
    }

    private static void line(Consumer<Component> chat, String format, Object... args) {

        if (chat == SILENT) {
            return;
        }
        String text = String.format(Locale.ROOT, format, args);
        Constants.LOG.info("[debug dump] {}", text);
        chat.accept(Component.literal(text));
    }

    /** Pass to {@link #build} for no chat or log lines. */
    public static final Consumer<Component> SILENT = line -> {
    };

    private static String fmt(Vec3 vec) {

        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
    }

    private static JsonArray vec(Vec3 vec) {

        JsonArray array = new JsonArray();
        array.add(vec.x);
        array.add(vec.y);
        array.add(vec.z);
        return array;
    }

    private static JsonElement pos(@Nullable BlockPos pos) {

        if (pos == null) {
            return JsonNull.INSTANCE;
        }
        JsonArray array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        return array;
    }
}
