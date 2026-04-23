package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.event.ClickTracker;
import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

public class CPSModule extends HudModule {
    private boolean showLabel;
    private boolean colorizeValue;

    public CPSModule() {
        super("CPS", "Affiche vos clics par seconde", 0, "CPS");
        this.showLabel = true;
        this.colorizeValue = true;
        registerCpsSettings();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        int cps = isPreviewRender() ? 11 : ClickTracker.getCPS();
        String text = showLabel ? cps + " CPS" : Integer.toString(cps);
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int color = !colorizeValue ? 0xFFFFFFFF : cps >= 14 ? 0xFF22C55E : cps >= 8 ? 0xFFF59E0B : 0xFFEF4444;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, color);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), color);
        endHudRender(guiGraphics);
    }

    private void registerCpsSettings() {
        addSetting(new BooleanModuleSetting(
            "cps_show_label",
            "Afficher le label",
            "Ajoute le suffixe CPS après la valeur.",
            "CPS",
            true,
            () -> showLabel,
            value -> showLabel = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "cps_colorize",
            "Coloriser",
            "Colore la valeur selon votre cadence de clic.",
            "CPS",
            true,
            () -> colorizeValue,
            value -> colorizeValue = value,
            true
        ));
    }
}
