package io.github.chillestorange.mixin.client;

import io.github.chillestorange.client.ui.SyncingScreen;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import io.github.chillestorange.util.WorldDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList.WorldListEntry;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(WorldListEntry.class)
public class WorldJoinMixin {

    private static final String LEVEL_DAT = "level.dat";

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelSummary summary;

    @Shadow
    @Final
    private WorldSelectionList list;

    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void worldsync$joinWorld(CallbackInfo ci) {

        String levelId = summary.getLevelId();
        if (!WorldSyncConfig.targetWorld().equals(levelId)) {
            return;
        }

        ci.cancel();

        Screen previousScreen = minecraft.screen;

        // TODO: Add functionality to cancel sync and return to world selection menu.
        minecraft.setScreen(new SyncingScreen());

        var worldPath = minecraft.getLevelSource().getLevelPath(levelId);
        WorldSyncLogger.info("Starting join-triggered sync for world {}.", levelId);

        WorldSyncService.runSyncCycle(
                worldPath,
                () -> worldsync$onSyncComplete(levelId, worldPath),
                error -> worldsync$onSyncFailed(previousScreen, levelId, error)
        );
    }

    @Unique
    private void worldsync$onSyncComplete(String levelId, Path worldPath) {
        var uuid = minecraft.getUser().getProfileId();
        var levelDatPath = worldPath.resolve(LEVEL_DAT);

        // Exceptions intentionally limited to those documented by
        // WorldDataHelper.updateSingleplayerUuid().
        try {
            WorldDataHelper.updateSingleplayerUuid(levelDatPath, uuid);
            WorldSyncLogger.debug("Updated singleplayer_uuid in level.dat to {} (path={}).", uuid, levelDatPath);
        } catch (IOException | IllegalStateException e) {
            WorldSyncLogger.error("Failed to update singleplayer_uuid in {}. Opening world anyway.", levelDatPath, e);
        }

        // TODO: Optionally add a feature to display sync completed on syncing screen (for UX)

        minecraft.execute(() ->
                minecraft.createWorldOpenFlows().openWorld(levelId, list::returnToScreen));
    }

    @Unique
    private void worldsync$onSyncFailed(Screen previousScreen, String levelId, Throwable error) {
        WorldSyncLogger.error("Sync failed for world {}.", levelId, error);

        minecraft.execute(() -> {
            if (minecraft.screen instanceof SyncingScreen) {
                minecraft.setScreen(previousScreen);
            }
        });
    }
}