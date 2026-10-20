package io.github.mdw22.paranormalcraft.item;

import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import io.github.mdw22.paranormalcraft.journal.JournalStages;
import io.github.mdw22.paranormalcraft.network.JournalPayload;
import io.github.mdw22.paranormalcraft.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * The Field Journal: use it to read it. The server sends the player's stages for each ghost type (hidden
 * advancements, see {@link JournalStages}) and their capture log; the client shows a read-only book.
 */
public class FieldJournalItem extends Item {

    public FieldJournalItem(Item.Properties properties) {

        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer);
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Sends the journal's contents to the player, which opens it on their client.
     */
    public static JournalPayload open(ServerPlayer player) {

        List<JournalPayload.TypeStage> stages = new ArrayList<>();
        for (String type : JournalStages.GHOST_TYPES) {
            stages.add(new JournalPayload.TypeStage(type, JournalStages.stage(player, type)));
        }
        JournalPayload payload = new JournalPayload(stages, CaptureLog.get(player.level().getServer()).entriesOf(player.getUUID()));
        Services.NETWORK.sendToPlayer(player, payload);
        ServerTrace.event("journal_open", line -> {
            line.addProperty("player", player.getGameProfile().name());
            stages.forEach(stage -> line.addProperty(stage.type(), stage.stage()));
            line.addProperty("captures", payload.captures().size());
        });
        return payload;
    }
}
