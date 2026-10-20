package io.github.mdw22.paranormalcraft.familiar;

import io.github.mdw22.paranormalcraft.Balance;

/**
 * The pure rule for the Wisp Familiar's light: it carries a vanilla light block ({@link #LEVEL}) where the light
 * around it, not counting its own, is at or below {@link #DARK_AT_OR_BELOW}.
 * <p>
 * The light engine can't say how much of a block's light is the wisp's own, so the rule works from what it can
 * know: the wisp's own light reaches a spot {@code LEVEL - distance} blocks away. Block light above that must come
 * from another source at least that bright; block light at or below it may be all the wisp's, so it counts as dark.
 * The result has a natural hysteresis: walking into a dim room keeps the light, walking up to a torch drops it.
 */
public final class WispLight {

    public static final int LEVEL = Balance.Familiar.LIGHT_LEVEL;
    public static final int DARK_AT_OR_BELOW = Balance.Familiar.DARK_AT_OR_BELOW;

    private WispLight() {
    }

    /**
     * @param blockLight    block light at the spot
     * @param skyLight      sky light at the spot, already darkened for the time of day
     * @param ownDistance   Manhattan distance from the wisp's current light block, or -1 if it has none
     * @return whether the spot is dark without the wisp's own light
     */
    public static boolean underlyingDark(int blockLight, int skyLight, int ownDistance) {

        if (skyLight > DARK_AT_OR_BELOW) {
            return false;
        }
        int own = ownDistance < 0 ? 0 : Math.max(0, LEVEL - ownDistance);
        int other = blockLight > own ? blockLight : 0;
        return other <= DARK_AT_OR_BELOW;
    }
}
