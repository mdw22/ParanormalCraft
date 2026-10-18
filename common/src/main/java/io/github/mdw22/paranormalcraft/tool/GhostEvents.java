package io.github.mdw22.paranormalcraft.tool;

import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hooks for things ghosts do that the tools react to.
 */
public final class GhostEvents {

    /** EMF holders within this many blocks of ghost activity spike to level 5. */
    public static final double ACTIVITY_RANGE = 16.0;

    private GhostEvents() {
    }

    /**
     * Called when a ghost does something noticeable (a manifest now; haunting actions later). Every EMF Reader holder
     * within {@link #ACTIVITY_RANGE} spikes to level 5 for 3 seconds.
     */
    public static void onGhostActivity(GhostEntity ghost) {

        if (!(ghost.level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(ghost) <= ACTIVITY_RANGE * ACTIVITY_RANGE && ToolTracker.isHolding(player, ModItems.EMF_READER.get())) {
                ToolTracker.spike(player);
            }
        }
    }
}
