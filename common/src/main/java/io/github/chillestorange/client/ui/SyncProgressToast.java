package io.github.chillestorange.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SyncProgressToast implements Toast {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/system");

    private static final long DISPLAY_TIME_MS = 5_000L;

    private static final int TITLE_X = 18;
    private static final int TITLE_Y = 7;

    private static final int DESCRIPTION_X = 18;
    private static final int DESCRIPTION_Y = 18;

    private static final int TITLE_COLOR = -256;
    private static final int DESCRIPTION_COLOR = -1;

    private final Component title;
    private final Component description;

    private Visibility visibility = Visibility.SHOW;

    public SyncProgressToast(Component title, Component description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        visibility = fullyVisibleForMs < DISPLAY_TIME_MS ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());

        graphics.text(font, title, TITLE_X, TITLE_Y, TITLE_COLOR, false);

        graphics.text(font, description, DESCRIPTION_X, DESCRIPTION_Y, DESCRIPTION_COLOR, false);
    }
}