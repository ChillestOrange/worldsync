package io.github.chillestorange.fabric.client.ui;

import io.github.chillestorange.client.ui.SyncHudRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;

public class FabricSyncHud implements HudElement {

    private final SyncHudRenderer renderer = new SyncHudRenderer();

    public static void register() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("worldsync", "sync_status"),
                new FabricSyncHud()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        renderer.render(graphics);
    }
}