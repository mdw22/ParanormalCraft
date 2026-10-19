package io.github.mdw22.paranormalcraft.tool;

import io.github.mdw22.paranormalcraft.Balance;
import io.github.mdw22.paranormalcraft.network.EmfLevelPayload;
import io.github.mdw22.paranormalcraft.network.ThermometerReadingPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side: every {@link #INTERVAL_TICKS}, each player holding a tool gets that tool's reading, sent to them alone.
 */
public final class ToolTracker {

    public static final int INTERVAL_TICKS = Balance.Tools.READING_INTERVAL_TICKS;
    /** How long ghost activity pins nearby readers at level 5. */
    public static final int SPIKE_TICKS = Balance.Tools.EMF_SPIKE_TICKS;

    // Player UUID -> game time until which their EMF reader is pinned at level 5. Server thread only.
    private static final Map<UUID, Long> EMF_SPIKES = new HashMap<>();

    private ToolTracker() {
    }

    public static boolean isHolding(Player player, Item item) {

        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    public static void tick(ServerPlayer player) {

        if (player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }
        if (isHolding(player, ModItems.EMF_READER.get())) {
            Services.NETWORK.sendToPlayer(player, new EmfLevelPayload(emfLevel(player)));
        }
        if (isHolding(player, ModItems.SPIRIT_THERMOMETER.get())) {
            float celsius = (float) ThermometerReadings.read(player.level(), player.position(), player.getRandom());
            Services.NETWORK.sendToPlayer(player, new ThermometerReadingPayload(celsius));
        }
    }

    /**
     * The level this player's reader shows right now, including any activity spike.
     */
    public static int emfLevel(ServerPlayer player) {

        Long spikeUntil = EMF_SPIKES.get(player.getUUID());
        if (spikeUntil != null) {
            if (player.level().getGameTime() < spikeUntil) {
                return EmfLevels.MAX_LEVEL;
            }
            EMF_SPIKES.remove(player.getUUID());
        }
        return EmfReadings.levelAt(player.level(), player.position());
    }

    /**
     * Pins this holder's reader at level 5 for {@link #SPIKE_TICKS} and tells them straight away.
     */
    public static void spike(ServerPlayer player) {

        EMF_SPIKES.put(player.getUUID(), player.level().getGameTime() + SPIKE_TICKS);
        Services.NETWORK.sendToPlayer(player, new EmfLevelPayload(EmfLevels.MAX_LEVEL));
    }

    /**
     * Whether this player's reader is pinned at level 5 by recent ghost activity.
     */
    public static boolean spikeActive(ServerPlayer player) {

        Long spikeUntil = EMF_SPIKES.get(player.getUUID());
        return spikeUntil != null && player.level().getGameTime() < spikeUntil;
    }

    public static void clear() {

        EMF_SPIKES.clear();
    }
}
