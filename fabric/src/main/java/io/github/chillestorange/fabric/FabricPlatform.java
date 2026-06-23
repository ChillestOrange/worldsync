package io.github.chillestorange.fabric;

import io.github.chillestorange.IPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric implementation of {@link IPlatform}, resolved via {@link java.util.ServiceLoader}. */
public final class FabricPlatform implements IPlatform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}