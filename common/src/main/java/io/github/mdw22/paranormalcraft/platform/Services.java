package io.github.mdw22.paranormalcraft.platform;

import io.github.mdw22.paranormalcraft.Constants;
import io.github.mdw22.paranormalcraft.platform.services.INetworkHelper;
import io.github.mdw22.paranormalcraft.platform.services.IPlatformHelper;
import io.github.mdw22.paranormalcraft.platform.services.IRegistrationHelper;

import java.util.ServiceLoader;

// Service loaders are a built-in Java feature that allow us to locate implementations of an interface that vary from one
// environment to another. In the context of MultiLoader we use this feature to access a mock API in the common code that
// is swapped out for the platform specific implementation at runtime.
/**
 * Loads the loader-specific service implementations named in each loader's {@code META-INF/services} files. Use
 * {@link #PLATFORM} and {@link #NETWORK} directly; registration goes through {@code registry.RegistrationHelper}.
 */
public class Services {

    // Information about the running platform: loader name, loaded mods, environment, config folder.
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    // Backs RegistrationHelper. Gameplay code should use RegistrationHelper, not this service directly.
    public static final IRegistrationHelper REGISTRATION = load(IRegistrationHelper.class);

    // Sends custom payloads. Payload types are declared in ModNetwork.
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform. For
    // example our file on NeoForge points to NeoForgePlatformHelper while Fabric points to FabricPlatformHelper.
    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
