package io.github.chillestorange.mixin.client;

import io.github.chillestorange.client.ui.SyncingScreen;
import io.github.chillestorange.config.WorldSyncConfig;
import io.github.chillestorange.logging.WorldSyncLogger;
import io.github.chillestorange.service.WorldSyncService;
import io.github.chillestorange.util.WorldDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList.WorldListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(WorldListEntry.class)
public class WorldJoinMixin {

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
        if (!WorldSyncConfig.targetWorld().equals(summary.getLevelName())) {
            return;
        }

        ci.cancel();

        Screen  PreviousScreen = minecraft.screen;
        minecraft.setScreen(new SyncingScreen());

        
        String levelId = summary.getLevelId();
        Path worldPath = minecraft.getLevelSource().getLevelPath(levelId);

        WorldSyncService.runSyncCycle(worldPath, () -> {
            var uuid = minecraft.getUser().getProfileId();

            Path levelDatPath = worldPath.resolve("level.dat");

            if (!WorldDataHelper.updateSingleplayerUuid(levelDatPath, uuid)) {
                WorldSyncLogger.error("Failed to update level.dat: path=" + levelDatPath);
                return;
            }

            WorldSyncLogger.info("Updated level.dat: path={}", levelDatPath);
            WorldSyncLogger.info("Opening synced world: id={}", levelId);

            minecraft.execute(() ->
                    minecraft.createWorldOpenFlows().openWorld(levelId, list::returnToScreen));
        });
    }
}