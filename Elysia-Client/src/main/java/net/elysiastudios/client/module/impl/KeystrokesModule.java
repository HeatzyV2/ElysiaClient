package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class KeystrokesModule extends HudModule {
    private boolean showMouseButtons;
    private boolean showSpacebar;
    private int keySize;

    public KeystrokesModule() {
        super("Keystrokes", "Affiche les touches pressées", 0, "⌨");
        this.showMouseButtons = true;
        this.showSpacebar = true;
        this.keySize = 22;
        registerKeystrokeSettings();
    }

    @Override
    public int getEditorWidth() {
        return keySize * 3 + 4;
    }

    @Override
    public int getEditorHeight() {
        int height = keySize * 2 + 2;
        if (showSpacebar) {
            height += 14;
        }
        if (showMouseButtons) {
            height += 16;
        }
        return height;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        int xOffset = 0;
        int gap = 2;
        int currentY = 0;

        beginHudRender(guiGraphics);
        boolean w = isPreviewRender() || mc.options.keyUp.isDown();
        boolean a = isPreviewRender() || mc.options.keyLeft.isDown();
        boolean s = !isPreviewRender() && mc.options.keyDown.isDown();
        boolean d = !isPreviewRender() && mc.options.keyRight.isDown();

        drawKey(guiGraphics, keySize + gap, currentY, keySize, "W", w);
        drawKey(guiGraphics, xOffset, currentY + keySize + gap, keySize, "A", a);
        drawKey(guiGraphics, keySize + gap, currentY + keySize + gap, keySize, "S", s);
        drawKey(guiGraphics, (keySize + gap) * 2, currentY + keySize + gap, keySize, "D", d);

        currentY += keySize * 2 + gap * 2;
        if (showSpacebar) {
            boolean space = isPreviewRender() || mc.options.keyJump.isDown();
            int width = keySize * 3 + gap * 2;
            guiGraphics.fill(0, currentY, width, currentY + 12, space ? resolveHudAccentColor(0xFFA855F7) : applyHudOpacity(0xAA101018));
            if (space) {
                guiGraphics.fill(0, currentY, width, currentY + 1, resolveHudAccentColor(0xFF7C3AED));
            }
            currentY += 14;
        }

        if (showMouseButtons) {
            long window = GLFW.glfwGetCurrentContext();
            boolean lmb = isPreviewRender() || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = !isPreviewRender() && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            int totalWidth = keySize * 3 + gap * 2;
            int halfWidth = (totalWidth - gap) / 2;

            guiGraphics.fill(0, currentY, halfWidth, currentY + 14, lmb ? resolveHudAccentColor(0xFFA855F7) : applyHudOpacity(0xAA101018));
            guiGraphics.drawCenteredString(mc.font, "LMB", halfWidth / 2, currentY + 3, lmb ? 0xFFFFFFFF : 0xFF94A3B8);
            guiGraphics.fill(halfWidth + gap, currentY, totalWidth, currentY + 14, rmb ? resolveHudAccentColor(0xFFA855F7) : applyHudOpacity(0xAA101018));
            guiGraphics.drawCenteredString(mc.font, "RMB", halfWidth + gap + halfWidth / 2, currentY + 3, rmb ? 0xFFFFFFFF : 0xFF94A3B8);
        }
        endHudRender(guiGraphics);
    }

    private void drawKey(GuiGraphics guiGraphics, int x, int y, int size, String key, boolean pressed) {
        guiGraphics.fill(x, y, x + size, y + size, pressed ? resolveHudAccentColor(0xFFA855F7) : applyHudOpacity(0xAA101018));
        if (pressed) {
            guiGraphics.fill(x, y, x + size, y + 1, resolveHudAccentColor(0xFF7C3AED));
        }
        guiGraphics.drawCenteredString(mc.font, key, x + size / 2, y + (size - 8) / 2, pressed ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void registerKeystrokeSettings() {
        addSetting(new IntModuleSetting(
            "keystrokes_size",
            "Taille des touches",
            "Ajuste la taille des touches affichées.",
            "Affichage",
            true,
            () -> keySize,
            value -> keySize = value,
            22,
            16,
            34,
            2,
            value -> value + " px"
        ));
        addSetting(new BooleanModuleSetting(
            "keystrokes_spacebar",
            "Afficher espace",
            "Affiche la barre espace sous WASD.",
            "Affichage",
            true,
            () -> showSpacebar,
            value -> showSpacebar = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "keystrokes_mouse",
            "Afficher souris",
            "Affiche les boutons gauche et droit.",
            "Affichage",
            true,
            () -> showMouseButtons,
            value -> showMouseButtons = value,
            true
        ));
    }
}
