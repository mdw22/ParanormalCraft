package io.github.mdw22.paranormalcraft.journal;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import io.github.mdw22.paranormalcraft.registry.ModTriggers;
import io.github.mdw22.paranormalcraft.tool.EmfLevels;
import io.github.mdw22.paranormalcraft.tool.Temperature;
import io.github.mdw22.paranormalcraft.tool.ThermometerReadings;
import io.github.mdw22.paranormalcraft.tool.ToolTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Fires the Field Journal and Paranormal Craft advancement events ({@link GhostEventTrigger}). Actions call
 * {@link #fire} directly (capture, containment, release, ward placement); {@link #tick} checks the three that come from
 * looking around (detected, seen, cold spot) every {@code ToolTracker} interval.
 */
public final class JournalEvents {

    private JournalEvents() {
    }

    public static String typeOf(Entity ghost) {

        return BuiltInRegistries.ENTITY_TYPE.getKey(ghost.getType()).toString();
    }

    /**
     * Fires {@code event} for {@code player}; processing or releasing a ghost also completes its "studied" stage.
     */
    public static void fire(ServerPlayer player, JournalEvent event, @Nullable String ghostType) {

        ModTriggers.GHOST_EVENT.get().trigger(player, event, Optional.ofNullable(ghostType));
        if (event == JournalEvent.PROCESSED || event == JournalEvent.RELEASED) {
            ModTriggers.GHOST_EVENT.get().trigger(player, JournalEvent.STUDIED, Optional.ofNullable(ghostType));
        }
    }

    /**
     * From {@code CommonEvents.onPlayerTick}: detected (EMF 5 with a ghost close by), seen (looking at one with
     * goggles) and cold spot (a cold reading at a cold spot).
     */
    public static void tick(ServerPlayer player) {

        if (player.tickCount % ToolTracker.INTERVAL_TICKS == 0) {
            check(player);
        }
    }

    /**
     * One round of the looking-around checks, now (tests call this directly).
     */
    public static void check(ServerPlayer player) {

        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.level();
        if (ToolTracker.isHolding(player, ModItems.EMF_READER.get()) && ToolTracker.emfLevel(player) == EmfLevels.MAX_LEVEL) {
            GhostEntity nearest = nearestGhost(level, player, Balance.Journal.DETECTED_RANGE);
            if (nearest != null) {
                fire(player, JournalEvent.DETECTED, typeOf(nearest));
            }
        }
        if (GhostVisibility.wearsGoggles(player)) {
            double range = Balance.Journal.SEEN_RANGE;
            for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, player.getBoundingBox().inflate(range))) {
                if (ghost.isAlive() && ghost.isLookedAtBy(player) && player.hasLineOfSight(ghost)) {
                    fire(player, JournalEvent.SEEN, typeOf(ghost));
                }
            }
        }
        if (ToolTracker.isHolding(player, ModItems.SPIRIT_THERMOMETER.get())
                && ThermometerReadings.readWithoutNoise(level, player.position()) < Balance.Journal.COLD_FEET_BELOW
                && ThermometerReadings.nearestColdSpotDistance(level, player.position()) <= Temperature.COLD_SPOT_RADIUS) {
            fire(player, JournalEvent.COLD_SPOT, null);
        }
    }

    /**
     * A capture: the captor's "captured" stage and Gotcha!, and a new capture log entry.
     */
    public static void captured(ServerPlayer captor, GhostEntity ghost) {

        String type = typeOf(ghost);
        fire(captor, JournalEvent.CAPTURED, type);
        CaptureLog.get(captor.level().getServer()).add(captor.getUUID(),
                new CaptureLog.Entry(ghost.getUUID(), ghost.ghostName(), type, captor.level().getGameTime() / 24000L, "stored"));
        ServerTrace.ghost("journal_capture_logged", ghost, line -> line.addProperty("player", captor.getGameProfile().name()));
    }

    private static @Nullable GhostEntity nearestGhost(ServerLevel level, ServerPlayer player, double range) {

        GhostEntity best = null;
        double bestSqr = range * range;
        for (GhostEntity ghost : level.getEntitiesOfClass(GhostEntity.class, new AABB(BlockPos.containing(player.position())).inflate(range))) {
            double distance = ghost.distanceToSqr(player);
            if (ghost.isAlive() && distance <= bestSqr) {
                best = ghost;
                bestSqr = distance;
            }
        }
        return best;
    }
}
