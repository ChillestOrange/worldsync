package io.github.chillestorange.neoforge;

import io.github.chillestorange.client.AutosaveSyncListener;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.neoforge.client.ui.NeoForgeSyncHud;
import io.github.chillestorange.service.WorldSyncService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(WorldSyncClientNeoForge.MOD_ID)
public class WorldSyncClientNeoForge {

    public static final String MOD_ID = "worldsync";

    public WorldSyncClientNeoForge(IEventBus modEventBus) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            throw new IllegalStateException(MOD_ID + " is a client-only mod and must not run on a dedicated server.");
        }
        modEventBus.addListener(WorldSyncClientNeoForge::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        WorldSyncConfig.HANDLER.load();
        WorldSyncLogger.setDebugEnabled(WorldSyncConfig.debugMode());
        AutosaveSyncListener.register();
        WorldSyncService.initialize(
                WorldSyncConfig.providerType(),
                WorldSyncConfig.credentials(),
                WorldSyncConfig.remoteFolderId(),
                WorldSyncConfig.configDir()
        );
    }
}
