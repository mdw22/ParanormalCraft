package io.github.mdw22.paranormalcraft.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkHelper {

    /**
     * Sends a play-to-client payload to one player. The payload type must be declared in
     * {@link io.github.mdw22.paranormalcraft.network.ModNetwork}.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
