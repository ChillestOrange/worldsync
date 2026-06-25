package io.github.chillestorange.fabric;

import io.github.chillestorange.client.AutosaveSyncListener;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.fabric.client.ui.FabricSyncHud;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import net.fabricmc.api.ClientModInitializer;

public final class WorldSyncClientFabric implements ClientModInitializer {

    public static final String MOD_ID = "worldsync";

    @Override
    public void onInitializeClient() {
        WorldSyncConfig.HANDLER.load();
        WorldSyncLogger.setDebugEnabled(WorldSyncConfig.debugMode());
        FabricSyncHud.register();
        AutosaveSyncListener.register();
        WorldSyncService.initialize(
                WorldSyncConfig.providerType(),
                WorldSyncConfig.credentials(),
                WorldSyncConfig.remoteFolderId(),
                WorldSyncConfig.configDir()
        );
    }
}