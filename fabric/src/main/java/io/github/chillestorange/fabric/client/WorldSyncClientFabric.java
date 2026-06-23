package io.github.chillestorange.fabric.client;

import io.github.chillestorange.client.AutosaveSyncListener;
import io.github.chillestorange.client.ui.SyncHudOverlay;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import net.fabricmc.api.ClientModInitializer;

public final class WorldSyncClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldSyncConfig.HANDLER.load();
        WorldSyncLogger.setDebugEnabled(WorldSyncConfig.debugMode());
        SyncHudOverlay.register();
        AutosaveSyncListener.register();
        WorldSyncService.initialize(
                WorldSyncConfig.providerType(),
                WorldSyncConfig.credentials(),
                WorldSyncConfig.remoteFolderId(),
                WorldSyncConfig.configDir()
        );
    }
}