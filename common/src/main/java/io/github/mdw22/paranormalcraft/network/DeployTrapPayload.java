package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player pressed attack while firing the Tether Wand. Vanilla swallows attack clicks while an
 * item is in use, so the client reports the key press and the server checks there is a latched tether
 * ({@code TetherManager.deployTrap}).
 */
public record DeployTrapPayload() implements CustomPacketPayload {

    public static final DeployTrapPayload INSTANCE = new DeployTrapPayload();
    public static final CustomPacketPayload.Type<DeployTrapPayload> TYPE = new CustomPacketPayload.Type<>(Constants.id("deploy_trap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeployTrapPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<DeployTrapPayload> type() {

        return TYPE;
    }
}
