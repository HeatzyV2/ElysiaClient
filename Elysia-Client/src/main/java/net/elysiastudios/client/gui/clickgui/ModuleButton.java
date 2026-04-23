package net.elysiastudios.client.gui.clickgui;

import net.elysiastudios.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ModuleButton {
    private final Module module;
    private int x, y;
    private final int width = 100;
    private final int height = 16;

    public ModuleButton(Module module, int x, int y) {
        this.module = module;
        this.x = x;
        this.y = y;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        int color = module.isEnabled() ? 0xAA6D28D9 : 0x00000000; // Fond violet si actif
        if (hovered) color = module.isEnabled() ? 0xCC7C3AED : 0x44FFFFFF;

        guiGraphics.fill(x, y, x + width, y + height, color);
        
        int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.drawString(Minecraft.getInstance().font, module.getIcon() + " " + module.getName(), x + 5, y + 4, textColor, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (button == 0) {
                module.toggle();
                return true;
            }
        }
        return false;
    }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
