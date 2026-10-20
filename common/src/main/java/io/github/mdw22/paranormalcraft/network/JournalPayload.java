package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.journal.CaptureLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client when the player opens their Field Journal: their stage for each ghost type and their capture log.
 * The client opens the journal screen with it.
 */
public record JournalPayload(List<TypeStage> stages, List<CaptureLog.Entry> captures) implements CustomPacketPayload {

    /**
     * @param stage 0 (never met) to 4 (studied)
     */
    public record TypeStage(String type, int stage) {

        public static final StreamCodec<RegistryFriendlyByteBuf, TypeStage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TypeStage::type,
                ByteBufCodecs.VAR_INT, TypeStage::stage,
                TypeStage::new);
    }

    public static final CustomPacketPayload.Type<JournalPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("journal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> STREAM_CODEC = StreamCodec.composite(
            TypeStage.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPayload::stages,
            CaptureLog.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPayload::captures,
            JournalPayload::new);

    @Override
    public CustomPacketPayload.Type<JournalPayload> type() {

        return TYPE;
    }
}
