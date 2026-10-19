package io.github.mdw22.paranormalcraft.client.tether;

import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.network.BeamStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of Tether beams: keeps the beams the server last sent and, every client tick, draws each as a jagged
 * soul-cyan crackle of particles from its owner's wand to the latched ghost (or the free end), with a looping hum at
 * the owner. Every player sees every beam in range. Beam changes go to the client trace as {@code beam_render}.
 */
public final class ClientBeams {

    private static final int CYAN = 0x6FE7F0;
    private static final DustParticleOptions DUST = new DustParticleOptions(CYAN, 0.6F);
    private static final double SPACING = 0.35;
    private static final double JAG = 0.08;

    private static List<BeamStatePayload.Beam> beams = List.of();
    private static final Map<Integer, String> LAST_TRACED = new HashMap<>();
    private static final Map<Integer, TetherHumSound> HUMS = new HashMap<>();

    private ClientBeams() {
    }

    public static void handle(BeamStatePayload payload) {

        beams = payload.beams();
    }

    /**
     * The beam a player is firing right now, if any (a test hook).
     */
    public static BeamStatePayload.@Nullable Beam beamOf(int playerId) {

        for (BeamStatePayload.Beam beam : beams) {
            if (beam.owner() == playerId) {
                return beam;
            }
        }
        return null;
    }

    public static boolean isFiring(Player player) {

        return beamOf(player.getId()) != null;
    }

    public static void tick(Minecraft minecraft) {

        ClientLevel level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }
        Map<Integer, BeamStatePayload.Beam> active = new HashMap<>();
        for (BeamStatePayload.Beam beam : beams) {
            if (!(level.getEntity(beam.owner()) instanceof Player owner)) {
                continue;
            }
            active.put(beam.owner(), beam);
            Entity target = beam.target() >= 0 ? level.getEntity(beam.target()) : null;
            Vec3 start = TetherManager.beamStart(owner);
            Vec3 end = target != null ? target.getBoundingBox().getCenter() : new Vec3(beam.endX(), beam.endY(), beam.endZ());
            draw(level, start, end, target != null);
            HUMS.computeIfAbsent(beam.owner(), id -> {
                TetherHumSound hum = new TetherHumSound(owner);
                minecraft.getSoundManager().play(hum);
                return hum;
            });
            trace(beam, owner, target, start, end);
        }
        for (Integer owner : List.copyOf(HUMS.keySet())) {
            if (!active.containsKey(owner)) {
                HUMS.remove(owner).finish();
            }
        }
        for (Integer owner : List.copyOf(LAST_TRACED.keySet())) {
            if (!active.containsKey(owner)) {
                LAST_TRACED.remove(owner);
                ClientTrace.event("beam_render", line -> {
                    line.addProperty("owner", owner);
                    line.addProperty("active", false);
                });
            }
        }
    }

    private static void draw(ClientLevel level, Vec3 start, Vec3 end, boolean latched) {

        RandomSource random = level.getRandom();
        Vec3 span = end.subtract(start);
        int steps = Math.max(2, (int) Math.ceil(span.length() / SPACING));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = start.add(span.scale((double) i / steps));
            double jag = i == 0 || i == steps ? 0.0 : JAG;
            double x = at.x + (random.nextDouble() - 0.5) * 2 * jag;
            double y = at.y + (random.nextDouble() - 0.5) * 2 * jag;
            double z = at.z + (random.nextDouble() - 0.5) * 2 * jag;
            level.addParticle(DUST, x, y, z, 0, 0, 0);
            if (random.nextInt(latched ? 3 : 6) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
            }
        }
    }

    private static void trace(BeamStatePayload.Beam beam, Player owner, @Nullable Entity target, Vec3 start, Vec3 end) {

        String state = beam.target() + "|" + (target != null);
        if (state.equals(LAST_TRACED.get(beam.owner()))) {
            return;
        }
        LAST_TRACED.put(beam.owner(), state);
        ClientTrace.event("beam_render", line -> {
            line.addProperty("owner", beam.owner());
            line.addProperty("ownerName", owner.getGameProfile().name());
            line.addProperty("active", true);
            line.addProperty("latched", target != null);
            line.addProperty("target", target == null ? "none" : target.getUUID().toString());
            line.addProperty("length", Math.round(start.distanceTo(end) * 100.0) / 100.0);
        });
    }

    public static void reset() {

        beams = List.of();
        LAST_TRACED.clear();
        HUMS.values().forEach(TetherHumSound::finish);
        HUMS.clear();
    }
}
