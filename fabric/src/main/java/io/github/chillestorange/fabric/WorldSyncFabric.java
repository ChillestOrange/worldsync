package io.github.chillestorange.fabric;

import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import net.fabricmc.api.ModInitializer;

public class WorldSyncFabric implements ModInitializer {

    public static final String MOD_ID = "worldsync";

    @Override
    public void onInitialize() {
        WorldSyncLogger.info("WorldSync Fabric has been initialized");
    }
}
