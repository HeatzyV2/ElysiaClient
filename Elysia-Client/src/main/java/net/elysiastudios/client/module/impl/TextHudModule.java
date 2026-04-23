package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

abstract class TextHudModule extends HudModule {
    private boolean uppercase;
    private boolean centeredText;
    private boolean compactMode;

    protected TextHudModule(String name, String description, String icon) {
        super(name, description, 0, icon);
        this.uppercase = false;
        this.centeredText = false;
        this.compactMode = false;
        registerTextHudSettings();
    }

    protected abstract String getText();

    protected String getPreviewText() {
        return getName();
    }

    protected int getAccentColor() {
        return 0xFF3B82F6;
    }

    protected int getTextColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public int getEditorWidth() {
        return Math.max(40, measureHudTextWidth(getDisplayText(true)));
    }

    @Override
    public int getEditorHeight() {
        return getTextBoxHeight();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        render(guiGraphics, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, boolean preview) {
        String text = getDisplayText(preview);
        if (text == null || text.isBlank()) {
            return;
        }

        int width = measureHudTextWidth(text);
        int height = getTextBoxHeight();
        int textWidth = mc.font.width(text);
        int textX = centeredText ? Math.max(getHudPadding(), (width - textWidth) / 2) : getHudPadding();

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, getAccentColor());
        drawHudText(guiGraphics, text, textX, getHudTextY(height), getTextColor());
        endHudRender(guiGraphics);
    }

    private void registerTextHudSettings() {
        addSetting(new BooleanModuleSetting(
            "text_uppercase",
            "Majuscules",
            "Affiche le texte du widget en majuscules.",
            "Texte",
            true,
            () -> uppercase,
            value -> uppercase = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "text_centered",
            "Texte centré",
            "Centre le texte à l'intérieur du widget.",
            "Texte",
            true,
            () -> centeredText,
            value -> centeredText = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "text_compact",
            "Mode compact",
            "Réduit légèrement la hauteur du widget.",
            "Texte",
            true,
            () -> compactMode,
            value -> compactMode = value,
            false
        ));
    }

    private String getDisplayText(boolean preview) {
        String text = getText();
        if ((text == null || text.isBlank()) && preview) {
            text = getPreviewText();
        }
        if (text == null) {
            return null;
        }
        return uppercase ? text.toUpperCase(Locale.ROOT) : text;
    }

    private int getTextBoxHeight() {
        return compactMode ? 12 : getHudBoxHeight();
    }
}
