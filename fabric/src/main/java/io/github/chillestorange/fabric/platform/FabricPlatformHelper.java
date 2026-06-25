package io.github.chillestorange.fabric.platform;

import io.github.chillestorange.platform.services.IPlatformHelper;
import io.github.chillestorange.platform.services.ModPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public ModPlatform getPlatform() {
        return ModPlatform.FABRIC;
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
