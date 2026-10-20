package io.github.mdw22.paranormalcraft.containment;

import io.github.mdw22.paranormalcraft.Balance;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * What each ghost type gives when processed or released, as plain data so it can be unit tested. Keys and values are
 * registry ids, resolved at the point of use; a new ghost type adds one row.
 */
public final class ContainmentTables {

    public static final double RESIDUE_CHANCE = Balance.Containment.RESIDUE_CHANCE;

    /**
     * Processing yields {@code min}–{@code max} of {@code item}.
     */
    public record Yield(String item, int min, int max) {
    }

    /**
     * Releasing gives the releasing player {@code effect} for {@code ticks}.
     */
    public record Blessing(String effect, int ticks) {
    }

    private static final Map<String, Yield> YIELDS = Map.of(
            "paranormalcraft:lingering_spirit", new Yield("paranormalcraft:lingering_ectoplasm",
                    Balance.Containment.LINGERING_YIELD_MIN, Balance.Containment.LINGERING_YIELD_MAX));

    private static final Map<String, Blessing> BLESSINGS = Map.of(
            "paranormalcraft:lingering_spirit", new Blessing("paranormalcraft:restful", Balance.Containment.RESTFUL_TICKS));

    private ContainmentTables() {
    }

    public static @Nullable Yield yieldFor(String ghostType) {

        return YIELDS.get(ghostType);
    }

    public static @Nullable Blessing blessingFor(String ghostType) {

        return BLESSINGS.get(ghostType);
    }

    /**
     * How many items a processed ghost yields, for a roll from 0 (inclusive) to 1 (exclusive).
     */
    public static int yieldCount(Yield yield, double roll) {

        int span = yield.max() - yield.min() + 1;
        return yield.min() + Math.min(span - 1, (int) Math.floor(roll * span));
    }

    /**
     * Whether a Spectral Residue drops too, for a roll from 0 to 1.
     */
    public static boolean residue(double roll) {

        return roll < RESIDUE_CHANCE;
    }
}
