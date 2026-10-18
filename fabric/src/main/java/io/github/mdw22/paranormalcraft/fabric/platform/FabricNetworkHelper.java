package io.github.mdw22.paranormalcraft.fabric.platform;

import io.github.mdw22.paranormalcraft.network.CommonPayloadRegistrar;
import io.github.mdw22.paranormalcraft.network.ModNetwork;
import io.github.mdw22.paranormalcraft.network.PayloadHandler;
import io.github.mdw22.paranormalcraft.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {

        if (player.connection != null && ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Called from the main initializer. Client-bound handlers are added in {@code ParanormalCraftFabricClient}.
     */
    public static void registerPayloads() {

        ModNetwork.registerPayloads(new CommonPayloadRegistrar() {

            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {

                PayloadTypeRegistry.clientboundPlay().register(type, codec);
            }

            @Override
            public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> handler) {

                PayloadTypeRegistry.serverboundPlay().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload, context.player()));
            }
        });
    }
}
