package io.github.mdw22.paranormalcraft.network;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.client.ClientToolState;
import io.github.mdw22.paranormalcraft.client.debug.ClientDebugTools;
import io.github.mdw22.paranormalcraft.client.debug.ClientTrace;
import io.github.mdw22.paranormalcraft.client.hallucination.ClientHallucinations;
import io.github.mdw22.paranormalcraft.debug.RevealDebug;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
    }
}
