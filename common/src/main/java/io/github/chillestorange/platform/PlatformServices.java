package io.github.chillestorange.platform;

import io.github.chillestorange.logging.GameSyncLogger;
import io.github.chillestorange.platform.services.IPlatformEvents;
import io.github.chillestorange.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class PlatformServices {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static final IPlatformEvents EVENTS = load(IPlatformEvents.class);

    private PlatformServices() {
    }

    private static <T> T load(Class<T> serviceClass) {
        T service = ServiceLoader.load(serviceClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to load platform service: " + serviceClass.getName()
                ));

        GameSyncLogger.debug("Loaded {} -> {}", serviceClass.getSimpleName(), service.getClass().getSimpleName());

        return service;
    }
}
