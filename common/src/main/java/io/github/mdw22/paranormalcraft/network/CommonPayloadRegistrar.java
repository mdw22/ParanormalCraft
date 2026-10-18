package io.github.mdw22.paranormalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Declares payload types on both sides. Implemented by each loader.
 */
public interface CommonPayloadRegistrar {

    /**
     * Declares a server-to-client payload. Its handler is registered separately in
     * {@link ModNetwork#registerClientHandlers(ClientPayloadRegistrar)}, which only runs on the client.
     */
    <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

    /**
     * Declares a client-to-server payload with its server-side handler.
     */
    <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler);
}
