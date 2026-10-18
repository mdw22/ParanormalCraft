package io.github.mdw22.paranormalcraft.config;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.Services;

/**
 * The loaded configs. Read once at startup; there is no reload yet.
 */
public final class ModConfigs {

    private static ServerConfig server = ServerConfig.DEFAULTS;
    private static ClientConfig client = ClientConfig.DEFAULTS;

    private ModConfigs() {
    }

    public static ServerConfig server() {

        return server;
    }

    public static ClientConfig client() {

        return client;
    }

    /**
     * Called from common init on both physical sides (the integrated server needs the server config too).
     */
    public static void loadServer() {

        server = ServerConfig.load(Services.PLATFORM.getConfigDir(), Constants.LOG);
    }

    /**
     * Called from client init only.
     */
    public static void loadClient() {

        client = ClientConfig.load(Services.PLATFORM.getConfigDir(), Constants.LOG);
    }
}
