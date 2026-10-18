package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the EMF level for the receiving player's own reader. Sent only to the holder, never stored on the
 * item, so other players can't see it.
 */
public record EmfLevelPayload(int level) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EmfLevelPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("emf_level"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmfLevelPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(EmfLevelPayload::new, EmfLevelPayload::level)
            .cast();

    @Override
    public CustomPacketPayload.Type<EmfLevelPayload> type() {

        return TYPE;
    }
}
