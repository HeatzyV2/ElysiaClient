package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class DirectionModule extends HudModule {
    public DirectionModule() {
        super("Direction", "Affiche la direction et l'angle du joueur.", 0, "DIR");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String text;
        if (isPreviewRender()) {
            text = "North  180 deg";
        } else {
            if (mc.player == null) {
                return;
            }
            float yaw = mc.player.getYRot();
            text = getFacing(yaw) + "  " + Math.round(yaw) + " deg";
        }

        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF38BDF8);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }

    private String getFacing(float yaw) {
        float normalized = (yaw % 360.0F + 360.0F) % 360.0F;
        if (normalized >= 45.0F && normalized < 135.0F) {
            return "West";
        }
        if (normalized >= 135.0F && normalized < 225.0F) {
            return "North";
        }
        if (normalized >= 225.0F && normalized < 315.0F) {
            return "East";
        }
        return "South";
    }
}
