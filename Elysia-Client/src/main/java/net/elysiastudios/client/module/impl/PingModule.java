package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class PingModule extends HudModule {
    public PingModule() {
        super("Ping", "Affiche votre latence au serveur", 0, "📡");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        int ping = 42;
        if (!isPreviewRender()) {
            if (mc.player == null || mc.getConnection() == null) {
                return;
            }
            var playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerInfo != null) {
                ping = playerInfo.getLatency();
            }
        }

        String text = ping + " ms";
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int color = ping < 50 ? 0xFF22C55E : ping < 100 ? 0xFFF59E0B : 0xFFEF4444;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, color);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), color);
        endHudRender(guiGraphics);
    }
}
