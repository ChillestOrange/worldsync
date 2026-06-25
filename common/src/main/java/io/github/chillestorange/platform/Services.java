package io.github.chillestorange.platform;

import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    private static <T> T load(Class<T> serviceClass) {
        T service = ServiceLoader.load(serviceClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to load platform service: " + serviceClass.getName()
                ));

        WorldSyncLogger.debug("Loaded {} -> {}", serviceClass.getSimpleName(), service.getClass().getSimpleName());

        return service;
    }
}
