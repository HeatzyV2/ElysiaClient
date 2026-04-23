package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class MemoryUsage extends HudModule {
    public MemoryUsage() {
        super("MemoryUsage", "Affiche l'utilisation de la RAM.", 0, "💾");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long used = isPreviewRender() ? Math.min(max, Math.max(512, max / 2)) : total - free;

        String text = "RAM: " + used + "MB / " + max + "MB";
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int color = used * 100L / Math.max(1L, max) >= 80 ? 0xFFEF4444 : 0xFFFFFFFF;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFFA855F7);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), color);
        endHudRender(guiGraphics);
    }
}
