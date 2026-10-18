package io.github.mdw22.paranormalcraft.neoforge;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.network.ClientPayloadRegistrar;
import io.github.mdw22.paranormalcraft.network.ModNetwork;
import io.github.mdw22.paranormalcraft.network.PayloadHandler;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ParanormalCraftNeoForgeClient {

    public ParanormalCraftNeoForgeClient(IEventBus modBus) {

        modBus.addListener(ParanormalCraftNeoForgeClient::registerClientPayloadHandlers);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {

        ModNetwork.registerClientHandlers(new ClientPayloadRegistrar() {

            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, PayloadHandler<T> handler) {

                event.register(type, (payload, context) -> handler.handle(payload, context.player()));
            }
        });
    }
}
