package io.github.mdw22.paranormalcraft.fabric;

import io.github.mdw22.paranormalcraft.client.ParanormalCraftClient;
import io.github.mdw22.paranormalcraft.network.ClientPayloadRegistrar;
import io.github.mdw22.paranormalcraft.network.ModNetwork;
import io.github.mdw22.paranormalcraft.network.PayloadHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Fabric client entrypoint: runs {@code ParanormalCraftClient.init()}, registers entity renderers and client payload
 * handlers, and forwards the client tick.
 */
public class ParanormalCraftFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ParanormalCraftClient.init();
        // Fabric's transitive access wideners make the vanilla registration method public.
        ParanormalCraftClient.registerEntityRenderers(EntityRenderers::register);
        ClientTickEvents.END_CLIENT_TICK.register(ParanormalCraftClient::tick);

        ModNetwork.registerClientHandlers(new ClientPayloadRegistrar() {

            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, PayloadHandler<T> handler) {

                ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.handle(payload, context.player()));
            }
        });
    }
}
