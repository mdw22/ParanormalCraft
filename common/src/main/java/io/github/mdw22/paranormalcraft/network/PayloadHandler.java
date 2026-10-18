package io.github.mdw22.paranormalcraft.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * Handles a received payload on the game thread. {@code player} is the sender on the server and the local player on
 * the client.
 */
@FunctionalInterface
public interface PayloadHandler<T extends CustomPacketPayload> {

    void handle(T payload, Player player);
}
