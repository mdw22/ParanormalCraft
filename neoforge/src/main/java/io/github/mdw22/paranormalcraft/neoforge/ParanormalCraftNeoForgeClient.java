package io.github.mdw22.paranormalcraft.neoforge;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.client.ParanormalCraftClient;
import io.github.mdw22.paranormalcraft.neoforge.platform.NeoForgeClientPlatformHelper;
import io.github.mdw22.paranormalcraft.network.ClientPayloadRegistrar;
import io.github.mdw22.paranormalcraft.network.ModNetwork;
import io.github.mdw22.paranormalcraft.network.PayloadHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ParanormalCraftNeoForgeClient {

    public ParanormalCraftNeoForgeClient(IEventBus modBus) {

        ParanormalCraftClient.init();
        modBus.addListener(ParanormalCraftNeoForgeClient::registerClientPayloadHandlers);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> ParanormalCraftClient.registerEntityRenderers(event::registerEntityRenderer));
        modBus.addListener(NeoForgeClientPlatformHelper::registerRangeSelectProperties);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ParanormalCraftClient.tick(Minecraft.getInstance()));
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
