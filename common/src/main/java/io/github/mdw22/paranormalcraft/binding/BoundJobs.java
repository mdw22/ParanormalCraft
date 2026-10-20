package io.github.mdw22.paranormalcraft.binding;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The job a bound ghost does, by ghost type, as plain data (the binding counterpart of {@code ContainmentTables}). A
 * ghost type without a row can't be bound. A new ghost type adds one row; a new job also needs its behaviour wired
 * into {@link SpiritVesselItem#use}.
 */
public final class BoundJobs {

    /** How the bound vessel is used: kept in the inventory as a companion, or placed as a block. */
    public enum Use {
        CARRIED,
        PLACED
    }

    /**
     * @param id  the job id, also its lang key ({@code job.paranormalcraft.<path>})
     * @param use carried or placed
     */
    public record Job(String id, Use use) {

        public String langKey() {

            return "job." + this.id.replace(':', '.');
        }
    }

    public static final String WISP_FAMILIAR = "paranormalcraft:wisp_familiar";

    private static final Map<String, Job> JOBS = Map.of(
            "paranormalcraft:lingering_spirit", new Job(WISP_FAMILIAR, Use.CARRIED));

    private BoundJobs() {
    }

    public static @Nullable Job jobFor(String ghostType) {

        return JOBS.get(ghostType);
    }

    public static boolean canBind(String ghostType) {

        return JOBS.containsKey(ghostType);
    }
}
