package io.github.mdw22.paranormalcraft.network;

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
    }

    /**
     * Called once per loader, on the physical client only.
     */
    public static void registerClientHandlers(ClientPayloadRegistrar registrar) {
    }
}
