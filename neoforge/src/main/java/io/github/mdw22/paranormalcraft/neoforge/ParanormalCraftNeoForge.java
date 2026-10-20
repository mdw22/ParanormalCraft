package io.github.mdw22.paranormalcraft.neoforge;

import io.github.mdw22.paranormalcraft.CommonEvents;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.ParanormalCraft;
import io.github.mdw22.paranormalcraft.neoforge.datagen.ModDataGenerators;
import io.github.mdw22.paranormalcraft.neoforge.platform.NeoForgeNetworkHelper;
import io.github.mdw22.paranormalcraft.neoforge.platform.NeoForgeRegistrationHelper;
import io.github.mdw22.paranormalcraft.registry.RegistrationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge mod entrypoint: sets the mod bus for registration, runs {@code ParanormalCraft.init()}, registers payloads,
 * entity attributes and datagen, and forwards server events to {@code CommonEvents}.
 */
@Mod(Constants.MOD_ID)
public class ParanormalCraftNeoForge {

    public ParanormalCraftNeoForge(IEventBus modBus) {

        // DeferredRegisters created during common init attach to this bus.
        NeoForgeRegistrationHelper.setModBus(modBus);
        ParanormalCraft.init();

        modBus.addListener(NeoForgeNetworkHelper::registerPayloads);
        modBus.addListener(ModDataGenerators::gatherClientData);
        modBus.addListener(ParanormalCraftNeoForge::createEntityAttributes);

        // Forward game events to CommonEvents, server side only.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> CommonEvents.onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonEvents.onPlayerTick(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                CommonEvents.onEntityJoinLevel(event.getEntity(), level);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> CommonEvents.onRegisterCommands(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> CommonEvents.onServerStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> CommonEvents.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                CommonEvents.onPlayerLogout(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CommonEvents.onServerStopped(event.getServer()));
    }

    private static void createEntityAttributes(EntityAttributeCreationEvent event) {

        for (RegistrationHelper.EntityAttributes<?> entry : RegistrationHelper.entityAttributes()) {
            event.put(entry.type().get(), entry.attributes().get().build());
        }
    }
}
