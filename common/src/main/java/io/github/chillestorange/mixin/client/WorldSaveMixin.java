package io.github.chillestorange.mixin.client;

import io.github.chillestorange.client.ui.SyncingScreen;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class WorldSaveMixin {

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void worldsync$stopServer(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        if (!(server instanceof IntegratedServer)) {
            return;
        }

        String levelId = storageSource.getLevelId();
        if (!WorldSyncConfig.targetWorld().equals(levelId)) {
            return;
        }

        var worldPath = storageSource.getLevelPath(LevelResource.ROOT);

        WorldSyncLogger.info("Target world closed, starting upload sync for world {}", levelId);

        var minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new SyncingScreen()));

        WorldSyncService.runSyncCycle(
                worldPath,
                () -> {
                    WorldSyncLogger.info("Upload sync complete for world {}", levelId);
                    worldsync$returnToTitleIfStillSyncing(minecraft);
                },
                error -> {
                    WorldSyncLogger.error("Upload sync failed for world {}", levelId, error);
                    worldsync$returnToTitleIfStillSyncing(minecraft);
                }
        );
    }

    @Unique
    private void worldsync$returnToTitleIfStillSyncing(Minecraft minecraft) {
        minecraft.execute(() -> {
            if (minecraft.screen instanceof SyncingScreen) {
                minecraft.setScreen(new TitleScreen());
            }
        });
    }
}