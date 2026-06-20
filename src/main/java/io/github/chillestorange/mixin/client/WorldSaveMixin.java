package io.github.chillestorange.mixin.client;

import io.github.chillestorange.client.ui.SyncingScreen;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public class WorldSaveMixin {

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void worldsync$stopServerTail(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        if (!(server instanceof IntegratedServer)) {
            return;
        }

        String worldName = server.getWorldData().getLevelName();
        Path worldPath = FabricLoader.getInstance()
                .getGameDir()
                .resolve("saves")
                .resolve(worldName);

        if (!WorldSyncConfig.targetWorld().equals(worldName)) {
            return;
        }

        WorldSyncLogger.info("Target world detected, starting sync: world={}", worldName);

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new SyncingScreen()));

        WorldSyncService.runSyncCycle(worldPath, () -> {
            WorldSyncLogger.info("Upload sync complete: world={}", worldName);
            minecraft.execute(() -> minecraft.setScreen(new TitleScreen()));
        });
    }
}