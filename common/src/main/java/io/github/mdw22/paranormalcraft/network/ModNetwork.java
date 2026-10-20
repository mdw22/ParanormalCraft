package io.github.mdw22.paranormalcraft.network;

import com.google.gson.JsonParser;
import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.capture.TetherManager;
import io.github.mdw22.paranormalcraft.client.ClientToolState;
import io.github.mdw22.paranormalcraft.client.containment.ContainmentUnitScreen;
import io.github.mdw22.paranormalcraft.client.debug.ClientDebugTools;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.client.hallucination.ClientHallucinations;
import io.github.mdw22.paranormalcraft.client.journal.JournalScreen;
import io.github.mdw22.paranormalcraft.client.tether.ClientBeams;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import io.github.mdw22.paranormalcraft.debug.trace.ServerTrace;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one place custom payloads are declared. Send them with {@code Services.NETWORK}.
 * <p>
 * To add a server-to-client payload: declare it in {@link #registerPayloads}, then add its handler in
 * {@link #registerClientHandlers}. Client handlers may call client-only code; nothing else in common may.
 */
public final class ModNetwork {

    public static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    /**
     * Called once per loader on both physical sides.
     */
    public static void registerPayloads(CommonPayloadRegistrar registrar) {

        registrar.playToClient(RevealFlagPayload.TYPE, RevealFlagPayload.STREAM_CODEC);
        registrar.playToClient(EmfLevelPayload.TYPE, EmfLevelPayload.STREAM_CODEC);
        registrar.playToClient(ThermometerReadingPayload.TYPE, ThermometerReadingPayload.STREAM_CODEC);
        registrar.playToClient(HallucinationPayload.TYPE, HallucinationPayload.STREAM_CODEC);
        registrar.playToClient(DebugClientPayload.TYPE, DebugClientPayload.STREAM_CODEC);
        registrar.playToClient(BeamStatePayload.TYPE, BeamStatePayload.STREAM_CODEC);
        registrar.playToClient(ContainmentStatePayload.TYPE, ContainmentStatePayload.STREAM_CODEC);
        registrar.playToClient(JournalPayload.TYPE, JournalPayload.STREAM_CODEC);
        registrar.playToServer(FigureResultPayload.TYPE, FigureResultPayload.STREAM_CODEC, (payload, player) -> {
            Constants.LOG.info("Shadow figure for {}: {}", player.getGameProfile().name(), payload.result());
            ServerTrace.event("figure_result", line -> {
                line.addProperty("player", player.getGameProfile().name());
                line.addProperty("result", payload.result());
            });
        });
        registrar.playToServer(TraceMirrorPayload.TYPE, TraceMirrorPayload.STREAM_CODEC, (payload, player) -> {
            try {
                ServerTrace.addClientLine(player.getGameProfile().name(), JsonParser.parseString(payload.json()).getAsJsonObject());
            } catch (RuntimeException e) {
                Constants.LOG.warn("Bad mirrored trace line from {}: {}", player.getGameProfile().name(), e.getMessage());
            }
        });
        registrar.playToServer(DeployTrapPayload.TYPE, DeployTrapPayload.STREAM_CODEC, (payload, player) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                TetherManager.deployTrap(serverPlayer);
            }
        });
        registrar.playToServer(DebugReplyPayload.TYPE, DebugReplyPayload.STREAM_CODEC, (payload, player) ->
                Constants.LOG.info("[debug] {} replied: {}", player.getGameProfile().name(), payload.message()));
    }

    /**
     * Called once per loader, on the physical client only.
     */
    public static void registerClientHandlers(ClientPayloadRegistrar registrar) {

        ClientPayloadRegistrar traced = new ClientPayloadRegistrar() {

            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, PayloadHandler<T> handler) {

                registrar.playToClient(type, (payload, player) -> {
                    ClientTrace.payload(type.id().toString());
                    handler.handle(payload, player);
                });
            }
        };
        traced.playToClient(RevealFlagPayload.TYPE, (payload, player) -> RevealDebug.handleClientFlag(payload));
        traced.playToClient(EmfLevelPayload.TYPE, (payload, player) -> ClientToolState.onEmfLevel(payload));
        traced.playToClient(ThermometerReadingPayload.TYPE, (payload, player) -> ClientToolState.onThermometerReading(payload));
        traced.playToClient(HallucinationPayload.TYPE, (payload, player) -> ClientHallucinations.handle(payload));
        traced.playToClient(DebugClientPayload.TYPE, (payload, player) -> ClientDebugTools.handle(payload));
        traced.playToClient(JournalPayload.TYPE, (payload, player) -> JournalScreen.open(payload));
        // Containment rows arrive twice a second while the menu is open: not traced as payloads.
        registrar.playToClient(ContainmentStatePayload.TYPE, (payload, player) -> ContainmentUnitScreen.handle(payload));
        // Beams arrive every tick while active: not traced as payloads (beam_render records the changes).
        registrar.playToClient(BeamStatePayload.TYPE, (payload, player) -> ClientBeams.handle(payload));
    }
}
