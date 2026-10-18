package io.github.mdw22.paranormalcraft.client;

import io.github.mdw22.paranormalcraft.platform.Services;
import io.github.mdw22.paranormalcraft.platform.services.IClientPlatformHelper;

/**
 * Client-only services, kept apart from {@link Services} so a dedicated server never loads them.
 */
public final class ClientServices {

    public static final IClientPlatformHelper PLATFORM = Services.load(IClientPlatformHelper.class);

    private ClientServices() {
    }
}
