package net.elysiastudios.client.hud;

import net.elysiastudios.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VanillaHudTransform {
    private VanillaHudTransform() {
    }

    public static void push(GuiGraphics guiGraphics, String id) {
        ConfigManager.HudConfig config = ConfigManager.getInstance().getHudConfig(id, 0, 0, 1.0F, true);
        VanillaHudElement element = VanillaHudRegistry.getElement(id);
        int baseX = 0;
        int baseY = 0;

        if (element != null) {
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            baseX = element.getBaseX(width, height);
            baseY = element.getBaseY(width, height);
        }

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) (baseX + config.x), (float) (baseY + config.y));
        guiGraphics.pose().scale(config.scale, config.scale);
        guiGraphics.pose().translate((float) -baseX, (float) -baseY);
    }

    public static void pop(GuiGraphics guiGraphics) {
        guiGraphics.pose().popMatrix();
    }
}
