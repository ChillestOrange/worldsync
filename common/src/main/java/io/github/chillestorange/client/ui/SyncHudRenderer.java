package io.github.chillestorange.client.ui;

import io.github.chillestorange.service.WorldSyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SyncHudRenderer {

    private static final int PADDING = 6;
    private static final long COMPLETE_HOLD_MS = 2500; // how long "World synced" stays fully visible
    private static final long FADE_DURATION_MS = 600;  // how long the fade-out takes after that

    private static final int ACCENT_SYNCING = 0x4FA8FF; // blue
    private static final int ACCENT_DONE = 0x57D68D;    // green

    private boolean wasSyncing = false;
    private long syncEndedAtMs = -1;

    public void render(GuiGraphicsExtractor graphics) {
        boolean syncing = WorldSyncService.isSyncing();
        long now = System.currentTimeMillis();

        if (syncing) {
            wasSyncing = true;
            syncEndedAtMs = -1;
        } else if (wasSyncing && syncEndedAtMs < 0) {
            syncEndedAtMs = now; // mark the moment sync finished
        }

        if (!syncing && syncEndedAtMs < 0) return;

        String label;
        int accentColor;
        float alpha = 1f;

        if (syncing) {
            label = "Syncing world...";
            accentColor = ACCENT_SYNCING;
        } else {
            long sinceEnd = now - syncEndedAtMs;

            if (sinceEnd >= COMPLETE_HOLD_MS + FADE_DURATION_MS) {
                wasSyncing = false;
                syncEndedAtMs = -1;
                return; // done fading, stop drawing
            }

            label = "World synced";
            accentColor = ACCENT_DONE;

            if (sinceEnd > COMPLETE_HOLD_MS) {
                float fadeProgress = (sinceEnd - COMPLETE_HOLD_MS) / (float) FADE_DURATION_MS;
                alpha = 1f - Math.clamp(fadeProgress, 0f, 1f);
            }
        }

        drawBadge(graphics, label, accentColor, alpha, syncing, now);
    }

    private void drawBadge(GuiGraphicsExtractor graphics, String label, int accentColor, float alpha, boolean syncing, long now) {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(label);

        int dotSize = 6;
        int innerPadding = 8;
        int badgeWidth = innerPadding + dotSize + 8 + textWidth + innerPadding;
        int badgeHeight = 18;

        int x = graphics.guiWidth() - badgeWidth - PADDING;
        int y = PADDING;

        int a = clampAlpha(alpha);

        int backgroundColor = ((int) (0xC8 * alpha) << 24) | 0x17171C;
        int accentBarColor = (a << 24) | (accentColor & 0xFFFFFF);
        int textColor = (a << 24) | 0xFFFFFF;

        graphics.fill(x, y, x + badgeWidth, y + badgeHeight, backgroundColor);
        graphics.fill(x, y, x + 2, y + badgeHeight, accentBarColor); // left accent strip

        int dotX = x + innerPadding;
        int dotY = y + (badgeHeight - dotSize) / 2;
        graphics.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, dotColor(accentColor, alpha, syncing, now));

        int textX = dotX + dotSize + 8;
        int textY = y + 5;
        graphics.text(font, label, textX, textY, textColor);
    }

    private int dotColor(int accentColor, float alpha, boolean syncing, long now) {
        float brightness = 1f;
        if (syncing) {
            double pulse = (Math.sin(now / 250.0) + 1.0) / 2.0; // smooth pulse, ~1.5s cycle
            brightness = (float) (0.6 + 0.4 * pulse);
        }
        int r = (int) (((accentColor >> 16) & 0xFF) * brightness);
        int g = (int) (((accentColor >> 8) & 0xFF) * brightness);
        int b = (int) ((accentColor & 0xFF) * brightness);
        return (clampAlpha(alpha) << 24) | (r << 16) | (g << 8) | b;
    }

    private int clampAlpha(float alpha) {
        return Math.round(255 * Math.clamp(alpha, 0f, 1f));
    }
}