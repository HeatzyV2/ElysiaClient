package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class CoordinatesModule extends HudModule {
    public CoordinatesModule() {
        super("Coordinates", "Affiche vos coordonnées", 0, "🧭");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String text;
        if (isPreviewRender()) {
            text = "X: 124  Y: 64  Z: -318";
        } else {
            if (mc.player == null) {
                return;
            }
            text = "X: " + (int) mc.player.getX() + "  Y: " + (int) mc.player.getY() + "  Z: " + (int) mc.player.getZ();
        }

        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF38BDF8);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
