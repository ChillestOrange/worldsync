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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

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

        String levelId = summary.getLevelId();
        if (!WorldSyncConfig.targetWorld().equals(levelId)) {
            return;
        }

        ci.cancel();

        Screen PreviousScreen = minecraft.screen;
        minecraft.setScreen(new SyncingScreen());

        Path worldPath = minecraft.getLevelSource().getLevelPath(levelId);
        WorldSyncLogger.info("Starting join-triggered sync for world: {}", levelId);

        WorldSyncService.runSyncCycle(worldPath, () -> {
            UUID uuid = minecraft.getUser().getProfileId();

            Path levelDatPath = worldPath.resolve("level.dat");

            try {
                WorldDataHelper.updateSingleplayerUuid(levelDatPath, uuid);

                WorldSyncLogger.debug("Updated singleplayer_uuid in level.dat to {} (path={}).", uuid, levelDatPath);
            } catch (IOException | IllegalArgumentException e) {
                WorldSyncLogger.error("Failed to update singleplayer_uuid in level.dat. "
                        + "Opening world anyway: " + levelId, e);

                return;
            }

            minecraft.execute(() ->
                    minecraft.createWorldOpenFlows().openWorld(levelId, list::returnToScreen));
        });
    }
}