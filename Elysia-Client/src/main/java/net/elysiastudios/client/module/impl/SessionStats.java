package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class SessionStats extends HudModule {
    private final long startTime;

    public SessionStats() {
        super("SessionStats", "Affiche les statistiques de la session.", 0, "📈");
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        long duration = isPreviewRender() ? 512L : (System.currentTimeMillis() - startTime) / 1000L;
        String text = String.format("Session: %dm %ds", duration / 60, duration % 60);
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFFF59E0B);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
