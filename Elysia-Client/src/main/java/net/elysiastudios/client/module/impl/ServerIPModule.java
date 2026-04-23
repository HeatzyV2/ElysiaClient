package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class ServerIPModule extends HudModule {
    public ServerIPModule() {
        super("ServerIP", "Affiche l'IP du serveur actuel.", 0, "🌍");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String ip = isPreviewRender()
            ? "IP: play.elysia.gg"
            : "IP: " + (mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Solo");

        int width = measureHudTextWidth(ip);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF22C55E);
        drawHudText(guiGraphics, ip, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
