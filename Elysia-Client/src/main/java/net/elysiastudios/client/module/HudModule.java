package net.elysiastudios.client.module;

import net.elysiastudios.client.config.ConfigManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.EnumModuleSetting;
import net.elysiastudios.client.setting.FloatModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;

public abstract class HudModule extends Module implements HudWidget, HudWidgetProvider {
    private boolean previewRender;
    private boolean showHudBackground;
    private boolean showHudAccent;
    private boolean hudTextShadow;
    private float hudBackgroundOpacity;
    private int hudPadding;
    private HudPalette hudPalette;

    public HudModule(String name, String description, int keyBind, String icon) {
        super(name, description, Category.HUD, keyBind, icon);
        this.showHudBackground = true;
        this.showHudAccent = true;
        this.hudTextShadow = true;
        this.hudBackgroundOpacity = 0.66F;
        this.hudPadding = 5;
        this.hudPalette = HudPalette.AUTO;
        registerHudSettings();
    }

    public abstract void render(GuiGraphics guiGraphics);

    @Override
    public void render(GuiGraphics guiGraphics, boolean preview) {
        previewRender = preview;
        render(guiGraphics);
        previewRender = false;
    }

    @Override
    public String getHudId() {
        return getName().toLowerCase().replace(" ", "-");
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    public ConfigManager.HudConfig getConfig() {
        return ConfigManager.getInstance().getHudConfig(getHudId(), getDefaultX(), getDefaultY(), getDefaultScale(), isDefaultVisible());
    }

    public int getX() { return getConfig().x; }
    public int getY() { return getConfig().y; }
    public float getScale() { return getConfig().scale; }

    @Override
    public List<HudWidget> getHudWidgets() {
        return List.of(this);
    }

    private void registerHudSettings() {
        addSetting(new BooleanModuleSetting(
            "hud_visible",
            "Visible HUD",
            "Affiche ou masque ce widget dans le HUD.",
            "HUD",
            false,
            () -> getConfig().visible,
            value -> getConfig().visible = value,
            true
        ));
        addSetting(new FloatModuleSetting(
            "hud_scale",
            "Échelle HUD",
            "Taille du widget dans le HUD.",
            "HUD",
            false,
            () -> getConfig().scale,
            value -> getConfig().scale = value,
            getDefaultScale(),
            0.5F,
            2.5F,
            0.05F,
            value -> String.format(Locale.ROOT, "%.2f", value)
        ));
        addSetting(new IntModuleSetting(
            "hud_x",
            "Position X",
            "Décale le widget sur l'axe horizontal.",
            "HUD",
            false,
            () -> getConfig().x,
            value -> getConfig().x = value,
            getDefaultX(),
            0,
            5000,
            4,
            Integer::toString
        ));
        addSetting(new IntModuleSetting(
            "hud_y",
            "Position Y",
            "Décale le widget sur l'axe vertical.",
            "HUD",
            false,
            () -> getConfig().y,
            value -> getConfig().y = value,
            getDefaultY(),
            0,
            5000,
            4,
            Integer::toString
        ));
        addSetting(new BooleanModuleSetting(
            "hud_background",
            "Fond",
            "Affiche un fond discret derrière le widget.",
            "Style",
            true,
            () -> showHudBackground,
            value -> showHudBackground = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "hud_accent",
            "Accent",
            "Affiche une ligne d'accent premium sur le widget.",
            "Style",
            true,
            () -> showHudAccent,
            value -> showHudAccent = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "hud_text_shadow",
            "Ombre du texte",
            "Ajoute une ombre légère au texte du widget.",
            "Style",
            true,
            () -> hudTextShadow,
            value -> hudTextShadow = value,
            true
        ));
        addSetting(new FloatModuleSetting(
            "hud_background_opacity",
            "Opacité du fond",
            "Règle la transparence du fond du widget.",
            "Style",
            true,
            () -> hudBackgroundOpacity,
            value -> hudBackgroundOpacity = value,
            0.66F,
            0.10F,
            1.00F,
            0.05F,
            value -> String.format(Locale.ROOT, "%.0f%%", value * 100.0F)
        ));
        addSetting(new IntModuleSetting(
            "hud_padding",
            "Padding",
            "Ajuste l'espace intérieur du widget.",
            "Style",
            true,
            () -> hudPadding,
            value -> hudPadding = value,
            5,
            2,
            12,
            1,
            Integer::toString
        ));
        addSetting(new EnumModuleSetting<>(
            "hud_palette",
            "Palette",
            "Force une palette cohérente pour ce widget.",
            "Style",
            true,
            () -> hudPalette,
            value -> hudPalette = value,
            HudPalette.AUTO,
            HudPalette.values(),
            HudPalette::getLabel
        ));
    }

    public int getEditorWidth() {
        return Math.max(60, mc.font.width(getName()) + getHudPadding() * 2);
    }

    public int getEditorHeight() {
        return getHudBoxHeight();
    }

    protected boolean shouldShowHudBackground() {
        return showHudBackground;
    }

    protected boolean isPreviewRender() {
        return previewRender;
    }

    protected boolean shouldShowHudAccent() {
        return showHudAccent;
    }

    protected boolean shouldUseHudTextShadow() {
        return hudTextShadow;
    }

    protected float getHudBackgroundOpacity() {
        return hudBackgroundOpacity;
    }

    protected int getHudPadding() {
        return hudPadding;
    }

    protected int getHudTextY(int boxHeight) {
        return Math.max(2, (boxHeight - mc.font.lineHeight) / 2);
    }

    protected int measureHudTextWidth(String text) {
        return Math.max(getHudPadding() * 2 + 12, mc.font.width(text) + getHudPadding() * 2);
    }

    protected int getHudBoxHeight() {
        return Math.max(14, mc.font.lineHeight + getHudPadding() + 1);
    }

    protected void beginHudRender(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) getX(), (float) getY());
        guiGraphics.pose().scale(getScale(), getScale());
    }

    protected void endHudRender(GuiGraphics guiGraphics) {
        guiGraphics.pose().popMatrix();
    }

    protected void drawHudFrame(GuiGraphics guiGraphics, int width, int height, int accentColor) {
        if (shouldShowHudBackground()) {
            guiGraphics.fill(0, 0, width, height, applyHudOpacity(0xAA101018));
        }
        if (shouldShowHudAccent()) {
            guiGraphics.fill(0, 0, width, 1, resolveHudAccentColor(accentColor));
        }
    }

    protected void drawHudText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(mc.font, text, x, y, color, shouldUseHudTextShadow());
    }

    protected int resolveHudAccentColor(int defaultColor) {
        return hudPalette == HudPalette.AUTO ? defaultColor : hudPalette.getColor();
    }

    protected int applyHudOpacity(int color) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int sourceAlpha = baseAlpha == 0 ? 255 : baseAlpha;
        int alpha = Math.max(0, Math.min(255, Math.round(sourceAlpha * getHudBackgroundOpacity())));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    protected enum HudPalette {
        AUTO("Automatique", 0),
        AZURE("Azur", 0xFF38BDF8),
        EMERALD("Émeraude", 0xFF22C55E),
        AMBER("Ambre", 0xFFF59E0B),
        ROSE("Rose", 0xFFA855F7),
        RUBY("Rubis", 0xFFEF4444),
        SLATE("Ardoise", 0xFF94A3B8);

        private final String label;
        private final int color;

        HudPalette(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }
}
