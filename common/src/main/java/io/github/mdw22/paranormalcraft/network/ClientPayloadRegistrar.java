package io.github.mdw22.paranormalcraft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Registers client-side handlers for server-to-client payloads. Implemented by each loader's client code.
 */
public interface ClientPayloadRegistrar {

    <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, PayloadHandler<T> handler);
}
