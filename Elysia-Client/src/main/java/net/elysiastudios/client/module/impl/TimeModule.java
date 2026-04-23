package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeModule extends HudModule {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TimeModule() {
        super("Time", "Affiche l'heure actuelle", 0, "🕐");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String time = isPreviewRender() ? "21:37" : LocalTime.now().format(FORMATTER);
        int width = measureHudTextWidth(time);
        int height = getHudBoxHeight();

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF7C3AED);
        drawHudText(guiGraphics, time, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
