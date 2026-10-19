package io.github.mdw22.paranormalcraft.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends custom payloads to one player. Payload types are declared in {@code network.ModNetwork}. Sending a payload the
 * player's connection hasn't negotiated is skipped, not an error.
 */
public interface INetworkHelper {

    /**
     * Sends a play-to-client payload to one player. The payload type must be declared in
     * {@link io.github.mdw22.paranormalcraft.network.ModNetwork}. Does nothing if that player's connection hasn't
     * negotiated the payload's channel (e.g. a GameTest mock player).
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
