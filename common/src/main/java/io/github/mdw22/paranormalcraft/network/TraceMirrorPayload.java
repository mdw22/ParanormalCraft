package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: one client trace line (JSON), mirrored while a script started by this player is running, so the
 * script's {@code expect trace} can see client events too.
 */
public record TraceMirrorPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TraceMirrorPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("trace_mirror"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TraceMirrorPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .map(TraceMirrorPayload::new, TraceMirrorPayload::json)
            .cast();

    @Override
    public CustomPacketPayload.Type<TraceMirrorPayload> type() {

        return TYPE;
    }
}
