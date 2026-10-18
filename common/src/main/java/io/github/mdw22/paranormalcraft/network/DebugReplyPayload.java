package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the result of a {@link DebugClientPayload} (e.g. where a screenshot or trace was written). The
 * server logs it and shows it to the player in chat, so scripted and remote runs can find the file.
 */
public record DebugReplyPayload(String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DebugReplyPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("debug_reply"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugReplyPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(DebugReplyPayload::new, DebugReplyPayload::message)
            .cast();

    @Override
    public CustomPacketPayload.Type<DebugReplyPayload> type() {

        return TYPE;
    }
}
