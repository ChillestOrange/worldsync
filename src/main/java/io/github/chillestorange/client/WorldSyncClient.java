package io.github.chillestorange.client;

import io.github.chillestorange.client.ui.SyncHudOverlay;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.service.WorldSyncService;
import net.fabricmc.api.ClientModInitializer;

public final class WorldSyncClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldSyncConfig.HANDLER.load();
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