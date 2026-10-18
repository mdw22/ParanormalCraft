package io.github.mdw22.paranormalcraft.client.renderer;

import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostEntity;
import io.github.mdw22.paranormalcraft.entity.ghost.GhostVisibility;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * How opaque a ghost is drawn for the local player, shared by the renderer and the client trace.
 */
public final class GhostAlpha {

    /** Full opacity of a visible ghost. */
    public static final float OPACITY = 0.6F;

    private GhostAlpha() {
    }

    /**
     * Whether the viewer sees ghosts regardless of manifesting: goggles, debug reveal or spectator.
     */
    public static boolean seesAnyway(@Nullable Player viewer) {

        return viewer != null && (GhostVisibility.wearsGoggles(viewer) || RevealDebug.isRevealed(viewer) || viewer.isSpectator());
    }

    /**
     * {@link #OPACITY} for viewers who see it anyway; otherwise scaled by the manifest fade (in over 6 ticks, out
     * over 10 from wherever it was).
     */
    public static float alpha(GhostEntity ghost, @Nullable Player viewer, float partialTick) {

        if (seesAnyway(viewer)) {
            return OPACITY;
        }
        return OPACITY * ghost.manifestFade(partialTick);
    }
}
