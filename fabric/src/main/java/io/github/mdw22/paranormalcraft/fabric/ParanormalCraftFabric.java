package io.github.mdw22.paranormalcraft.fabric;

import io.github.mdw22.paranormalcraft.CommonEvents;
import io.github.mdw22.paranormalcraft.ParanormalCraft;
import io.github.mdw22.paranormalcraft.fabric.platform.FabricNetworkHelper;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric main entrypoint: runs {@code ParanormalCraft.init()}, registers payloads and entity attributes, and forwards
 * Fabric API server events to {@code CommonEvents}.
 */
public class ParanormalCraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        ParanormalCraft.init();
        FabricNetworkHelper.registerPayloads();
        for (RegistrationHelper.EntityAttributes<?> entry : RegistrationHelper.entityAttributes()) {
            FabricDefaultAttributeRegistry.register(entry.type().get(), entry.attributes().get());
        }

        // Forward game events to CommonEvents. Fabric has no player tick event, so players are ticked from the
        // end of the server tick, before the server tick handler, matching NeoForge's order.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                CommonEvents.onPlayerTick(player);
            }
            CommonEvents.onServerTick(server);
        });
        ServerEntityEvents.ENTITY_LOAD.register(CommonEvents::onEntityJoinLevel);
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> CommonEvents.onRegisterCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPED.register(CommonEvents::onServerStopped);
    }
}
