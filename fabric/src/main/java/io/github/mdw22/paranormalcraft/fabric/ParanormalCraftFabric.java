package io.github.mdw22.paranormalcraft.fabric;

import io.github.mdw22.paranormalcraft.CommonEvents;
import io.github.mdw22.paranormalcraft.ParanormalCraft;
import io.github.mdw22.paranormalcraft.fabric.platform.FabricNetworkHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

public class ParanormalCraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        ParanormalCraft.init();
        FabricNetworkHelper.registerPayloads();

        // Forward game events to CommonEvents. Fabric has no player tick event, so players are ticked from the
        // end of the server tick, before the server tick handler, matching NeoForge's order.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                CommonEvents.onPlayerTick(player);
            }
            CommonEvents.onServerTick(server);
        });
        ServerEntityEvents.ENTITY_LOAD.register(CommonEvents::onEntityJoinLevel);
    }
}
