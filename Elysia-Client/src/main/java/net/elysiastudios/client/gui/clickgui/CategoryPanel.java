package net.elysiastudios.client.gui.clickgui;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    private final Category category;
    private int x, y;
    private final int width = 100;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private boolean dragging;
    private int dragX, dragY;

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;

        int buttonY = y + 20;
        for (Module module : ModuleManager.getInstance().getModulesByCategory(category)) {
            buttons.add(new ModuleButton(module, x, buttonY));
            buttonY += 16;
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
            updateButtonPositions();
        }

        // Header
        guiGraphics.fill(x, y, x + width, y + 18, 0xFFA855F7); // Couleur accent Elysia
        guiGraphics.drawString(Minecraft.getInstance().font, category.getIcon() + " " + category.getName(), x + 5, y + 5, 0xFFFFFFFF, true);

        // Background des modules
        guiGraphics.fill(x, y + 18, x + width, y + 18 + (buttons.size() * 16), 0xDD101018);

        for (ModuleButton button : buttons) {
            button.render(guiGraphics, mouseX, mouseY);
        }
    }

    private void updateButtonPositions() {
        int buttonY = y + 18;
        for (ModuleButton button : buttons) {
            button.setX(x);
            button.setY(buttonY);
            buttonY += 16;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 18) {
            if (button == 0) {
                dragging = true;
                dragX = (int) (mouseX - x);
                dragY = (int) (mouseY - y);
                return true;
            }
        }
        for (ModuleButton moduleButton : buttons) {
            if (moduleButton.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
    }
}
