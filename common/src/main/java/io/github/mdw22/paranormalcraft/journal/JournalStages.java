package io.github.mdw22.paranormalcraft.journal;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Field Journal stages for each ghost type, stored as hidden advancements
 * {@code paranormalcraft:journal/<type path>/<stage>} (detected, seen, captured, studied), so they are per player,
 * saved and synced by vanilla. New ghost types add their id to {@link #GHOST_TYPES} and four advancements in datagen.
 */
public final class JournalStages {

    /** Every ghost type with a journal page, in page order. */
    public static final List<String> GHOST_TYPES = List.of("paranormalcraft:lingering_spirit");

    private JournalStages() {
    }

    /**
     * The advancement for {@code ghostType}'s stage {@code stage} (1 to 4).
     */
    public static Identifier advancementId(String ghostType, int stage) {

        String path = Identifier.parse(ghostType).getPath();
        return Constants.id("journal/" + path + "/" + JournalEvent.STAGES[stage - 1].serialName());
    }

    /**
     * The hidden advancement for having bound a ghost of {@code ghostType}: it adds the bound-job line to the page.
     */
    public static Identifier boundAdvancementId(String ghostType) {

        return Constants.id("journal/" + Identifier.parse(ghostType).getPath() + "/" + JournalEvent.BOUND.serialName());
    }

    /**
     * Whether the player has bound a ghost of {@code ghostType}.
     */
    public static boolean bound(ServerPlayer player, String ghostType) {

        AdvancementHolder holder = player.level().getServer().getAdvancements().get(boundAdvancementId(ghostType));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * The player's highest completed stage for {@code ghostType}, 0 to 4.
     */
    public static int stage(ServerPlayer player, String ghostType) {

        int stage = 0;
        for (int n = 1; n <= JournalEvent.STAGES.length; n++) {
            AdvancementHolder holder = holder(player, ghostType, n);
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                stage = n;
            }
        }
        return stage;
    }

    /**
     * Debug: completes stages 1..{@code stage} and clears the rest; stage 5 is all four plus the bound line.
     */
    public static void setStage(ServerPlayer player, String ghostType, int stage) {

        for (int n = 1; n <= JournalEvent.STAGES.length + 1; n++) {
            AdvancementHolder holder = n > JournalEvent.STAGES.length
                    ? player.level().getServer().getAdvancements().get(boundAdvancementId(ghostType))
                    : holder(player, ghostType, n);
            if (holder == null) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            if (n <= stage) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(holder, criterion);
                }
            } else {
                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revoke(holder, criterion);
                }
            }
        }
    }

    private static @Nullable AdvancementHolder holder(ServerPlayer player, String ghostType, int stage) {

        return player.level().getServer().getAdvancements().get(advancementId(ghostType, stage));
    }
}
