package io.github.chillestorange.neoforge.platform;

import io.github.chillestorange.platform.services.IPlatformHelper;
import io.github.chillestorange.platform.services.ModPlatform;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public ModPlatform getPlatform() {
        return ModPlatform.NEOFORGE;
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
