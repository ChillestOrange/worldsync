package io.github.chillestorange.client;

import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class AutosaveSyncListener {

    private AutosaveSyncListener() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AutosaveSyncListener::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        WorldSyncConfig config = WorldSyncConfig.HANDLER.instance();

        if (!config.autosaveSyncEnabled) {
            return;
        }

        if (!(server instanceof IntegratedServer)) {
            return;
        }

        int intervalTicks = config.autosaveIntervalTicks;
        if (intervalTicks <= 0 || server.getTickCount() % intervalTicks != 0) {
            return;
        }

        String worldName = server.getWorldData().getLevelName();
        if (!config.targetWorld.equals(worldName)) {
            return;
        }

        Path worldPath = FabricLoader.getInstance()
                .getGameDir()
                .resolve("saves")
                .resolve(worldName);

        WorldSyncLogger.info("Detected autosave tick for target world, starting sync: world=", worldName);

        WorldSyncService.runSyncCycle(worldPath);
    }
}