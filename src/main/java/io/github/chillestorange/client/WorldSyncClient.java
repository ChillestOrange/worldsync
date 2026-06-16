package io.github.chillestorange.client;

import net.fabricmc.api.ClientModInitializer;

public final class WorldSyncClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AutosaveSyncListener.register();
    }
}