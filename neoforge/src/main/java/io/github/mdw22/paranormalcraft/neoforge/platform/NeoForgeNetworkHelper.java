package io.github.mdw22.paranormalcraft.neoforge.platform;

import io.github.mdw22.paranormalcraft.network.CommonPayloadRegistrar;
import io.github.mdw22.paranormalcraft.network.ModNetwork;
import io.github.mdw22.paranormalcraft.network.PayloadHandler;
import io.github.mdw22.paranormalcraft.platform.services.INetworkHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Networking on NeoForge: payloads are registered on the mod bus ({@link #registerPayloads}) and sent with
 * {@code PacketDistributor}, only over channels the client negotiated.
 */
public class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {

        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * Mod bus listener. Client-bound handlers are added in {@code ParanormalCraftNeoForgeClient}.
     */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(ModNetwork.PROTOCOL_VERSION);
        ModNetwork.registerPayloads(new CommonPayloadRegistrar() {

            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {

                registrar.playToClient(type, codec);
            }

            @Override
            public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler) {

                registrar.playToServer(type, codec, (payload, context) -> handler.handle(payload, context.player()));
            }
        });
    }
}
