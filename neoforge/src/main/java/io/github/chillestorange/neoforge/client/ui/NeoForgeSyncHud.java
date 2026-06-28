package io.github.chillestorange.neoforge.client.ui;

import io.github.chillestorange.GameSyncConstants;
import io.github.chillestorange.client.ui.SyncHudRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = GameSyncConstants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeSyncHud {

    private static final SyncHudRenderer renderer = new SyncHudRenderer();

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(GameSyncConstants.MOD_ID, "sync_status"),
                (graphics, deltaTracker) ->
                        renderer.render(graphics)
        );
    }
}