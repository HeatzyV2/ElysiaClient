package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class FPSModule extends HudModule {
    public FPSModule() {
        super("FPS", "Affiche vos images par seconde", 0, "FPS");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        int fps = isPreviewRender() ? 144 : mc.getFps();
        String text = fps + " FPS";
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int color = fps >= 120 ? 0xFF22C55E : fps >= 60 ? 0xFFF59E0B : 0xFFEF4444;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, color);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), color);
        endHudRender(guiGraphics);
    }
}
