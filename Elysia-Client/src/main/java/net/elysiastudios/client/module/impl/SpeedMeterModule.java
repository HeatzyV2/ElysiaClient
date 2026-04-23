package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class SpeedMeterModule extends HudModule {
    public SpeedMeterModule() {
        super("SpeedMeter", "Affiche votre vitesse actuelle", 0, "⚡");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        double speed = 5.6D;
        if (!isPreviewRender()) {
            if (mc.player == null) {
                return;
            }
            double dx = mc.player.getX() - mc.player.xOld;
            double dz = mc.player.getZ() - mc.player.zOld;
            speed = Math.sqrt(dx * dx + dz * dz) * 20.0D;
        }

        String text = String.format(Locale.ROOT, "%.1f b/s", speed);
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF3B82F6);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
