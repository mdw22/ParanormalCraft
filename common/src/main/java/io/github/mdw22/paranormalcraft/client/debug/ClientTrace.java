package io.github.mdw22.paranormalcraft.client.debug;

import com.google.gson.JsonObject;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.client.ClientToolState;
import io.github.mdw22.paranormalcraft.client.hallucination.ClientHallucinations;
import io.github.mdw22.paranormalcraft.client.renderer.GhostAlpha;
import io.github.mdw22.paranormalcraft.debug.trace.TraceWriter;
import io.github.mdw22.paranormalcraft.entity.ShadowFigureEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.network.TraceMirrorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The client half of {@code /paranormalcraft debug trace}, written to
 * {@code <game dir>/paranormalcraft-trace/trace-<timestamp>.jsonl}: our mod's sounds as they play, ghost render state
 * when it changes (and every tick of a fade), shadow figures appearing and vanishing, EMF and thermometer readings,
 * payload types received, and goggles going on and off. Every hook is a no-op while no trace is open.
 */
public final class ClientTrace {

    /** Ghosts further than this from the local player aren't traced. */
    private static final double GHOST_RANGE = 64.0;

    private static @Nullable TraceWriter writer;
    /** While a script this player started is running, every line also goes to the server for {@code expect trace}. */
    private static boolean mirroring;
    private static boolean soundListenerAdded;
    /** Set just before our own client-only code plays a sound, so the listener can mark it local. */
    private static boolean nextSoundLocal;
    private static final Map<UUID, String> LAST_GHOST_STATE = new HashMap<>();
    private static @Nullable Boolean lastGoggles;

    private ClientTrace() {
    }

    public static boolean active() {

        return writer != null || mirroring;
    }

    public static void setMirroring(boolean mirror) {

        mirroring = mirror;
        if (mirror) {
            LAST_GHOST_STATE.clear();
            lastGoggles = null;
        }
    }

    public static Path start() throws IOException {

        stop();
        writer = TraceWriter.open(Minecraft.getInstance().gameDirectory.toPath(), "trace-");
        LAST_GHOST_STATE.clear();
        lastGoggles = null;
        event("trace_start", line -> {
        });
        return writer.path();
    }

    public static @Nullable Path stop() {

        if (writer == null) {
            return null;
        }
        event("trace_stop", line -> {
        });
        Path path = writer.path();
        writer.close();
        writer = null;
        return path;
    }

    public static void event(String type, Consumer<JsonObject> fields) {

        if (!active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        JsonObject line = TraceWriter.event(minecraft.level == null ? -1 : minecraft.level.getGameTime(), type);
        fields.accept(line);
        if (writer != null) {
            writer.write(line);
        }
        if (mirroring && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new TraceMirrorPayload(line.toString())));
        }
    }

    public static void addPos(JsonObject line, Entity entity) {

        line.addProperty("x", round(entity.getX()));
        line.addProperty("y", round(entity.getY()));
        line.addProperty("z", round(entity.getZ()));
    }

    private static double round(double value) {

        return Math.round(value * 1000.0) / 1000.0;
    }

    /**
     * Call right before our client-only code plays a sound (e.g. the shadow figure's), so the trace marks it local.
     */
    public static void markNextSoundLocal() {

        nextSoundLocal = true;
    }

    /**
     * Called once per client tick: hooks the sound listener once, and records goggles and ghost state changes.
     */
    public static void tick(Minecraft minecraft) {

        if (!soundListenerAdded) {
            minecraft.getSoundManager().addListener(ClientTrace::onSound);
            soundListenerAdded = true;
        }
        nextSoundLocal = false;
        if (!active() || minecraft.level == null || minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        boolean goggles = GhostVisibility.wearsGoggles(player);
        if (lastGoggles == null || lastGoggles != goggles) {
            lastGoggles = goggles;
            event(goggles ? "goggles_on" : "goggles_off", line -> {
            });
        }
        Set<UUID> seen = new HashSet<>();
        for (GhostEntity ghost : minecraft.level.getEntitiesOfClass(GhostEntity.class, new AABB(player.blockPosition()).inflate(GHOST_RANGE))) {
            seen.add(ghost.getUUID());
            float alpha = GhostAlpha.alpha(ghost, player, 1.0F);
            boolean visible = !ghost.isInvisible();
            boolean fading = ghost.isFadingOut() || (ghost.isManifesting() && ghost.manifestFade(1.0F) < 1.0F && !GhostAlpha.seesAnyway(player));
            String state = String.format(java.util.Locale.ROOT, "%.3f|%s|%s|%s", alpha, visible, goggles, ghost.isManifesting());
            if (fading || !state.equals(LAST_GHOST_STATE.get(ghost.getUUID()))) {
                LAST_GHOST_STATE.put(ghost.getUUID(), state);
                event("ghost_render", line -> {
                    line.addProperty("ghost", ghost.getUUID().toString());
                    line.addProperty("alpha", round(alpha));
                    line.addProperty("visible", visible);
                    line.addProperty("tinted", goggles);
                    line.addProperty("manifesting", ghost.isManifesting());
                    line.addProperty("fading", fading);
                    line.addProperty("pickable", ghost.isPickable());
                });
            }
        }
        LAST_GHOST_STATE.keySet().retainAll(seen);
    }

    private static void onSound(SoundInstance sound, WeighedSoundEvents soundEvent, float range) {

        if (!active() || !Constants.MOD_ID.equals(sound.getIdentifier().getNamespace())) {
            return;
        }
        boolean local = nextSoundLocal;
        nextSoundLocal = false;
        event("sound", line -> {
            line.addProperty("id", sound.getIdentifier().toString());
            line.addProperty("x", round(sound.getX()));
            line.addProperty("y", round(sound.getY()));
            line.addProperty("z", round(sound.getZ()));
            line.addProperty("volume", round(sound.getVolume()));
            line.addProperty("pitch", round(sound.getPitch()));
            line.addProperty("relative", sound.isRelative());
            line.addProperty("range", round(range));
            line.addProperty("local", local);
        });
    }

    public static void payload(String type) {

        event("payload", line -> line.addProperty("payload", type));
    }

    /**
     * One line with the whole client state: local player, goggles, every ghost in range, the active figure, EMF and
     * thermometer.
     */
    public static void snapshot(Minecraft minecraft) {

        event("snapshot", line -> {
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.level == null) {
                return;
            }
            JsonObject local = new JsonObject();
            local.addProperty("name", player.getGameProfile().name());
            addPos(local, player);
            local.addProperty("yaw", round(player.getYRot()));
            local.addProperty("pitch", round(player.getXRot()));
            local.addProperty("goggles", GhostVisibility.wearsGoggles(player));
            local.addProperty("spectator", player.isSpectator());
            line.add("player", local);
            com.google.gson.JsonArray ghosts = new com.google.gson.JsonArray();
            for (GhostEntity ghost : minecraft.level.getEntitiesOfClass(GhostEntity.class, new AABB(player.blockPosition()).inflate(GHOST_RANGE))) {
                JsonObject entry = new JsonObject();
                entry.addProperty("uuid", ghost.getUUID().toString());
                addPos(entry, ghost);
                entry.addProperty("alpha", round(GhostAlpha.alpha(ghost, player, 1.0F)));
                entry.addProperty("visible", !ghost.isInvisible());
                entry.addProperty("pickable", ghost.isPickable());
                entry.addProperty("manifesting", ghost.isManifesting());
                entry.addProperty("presence", ghost.getPresence());
                ghosts.add(entry);
            }
            line.add("ghosts", ghosts);
            ShadowFigureEntity figure = ClientHallucinations.active();
            if (figure != null) {
                JsonObject entry = new JsonObject();
                addPos(entry, figure);
                entry.addProperty("age", figure.tickCount);
                line.add("figure", entry);
            }
            line.addProperty("emf", ClientToolState.emfLevel());
            line.addProperty("thermometer", ClientToolState.lastCelsius());
        });
    }

    public static void reset() {

        LAST_GHOST_STATE.clear();
        lastGoggles = null;
        mirroring = false;
    }
}
