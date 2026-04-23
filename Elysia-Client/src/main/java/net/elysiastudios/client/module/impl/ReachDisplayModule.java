package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class ReachDisplayModule extends HudModule {
    private static float lastReach;
    private static long lastHitTime;

    private int fadeSeconds;
    private int decimals;
    private boolean showUnit;

    public ReachDisplayModule() {
        super("ReachDisplay", "Affiche la distance de frappe", 0, "📏");
        this.fadeSeconds = 3;
        this.decimals = 2;
        this.showUnit = true;
        registerReachSettings();
    }

    public static void onHit(float distance) {
        lastReach = distance;
        lastHitTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        long visibleWindow = fadeSeconds * 1000L;
        long elapsed = isPreviewRender() ? 450L : System.currentTimeMillis() - lastHitTime;
        float reach = isPreviewRender() ? 3.08F : lastReach;
        if (elapsed > visibleWindow) {
            return;
        }

        String format = "%." + decimals + "f";
        String text = String.format(Locale.ROOT, format, reach) + (showUnit ? " blocks" : "");
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int alpha = (int) (Math.max(0.0D, 1.0D - (elapsed / (double) visibleWindow)) * 255.0D);
        if (alpha <= 10) {
            return;
        }

        beginHudRender(guiGraphics);
        if (shouldShowHudBackground()) {
            guiGraphics.fill(0, 0, width, height, applyHudOpacity((alpha << 24) | 0x00101018));
        }
        if (shouldShowHudAccent()) {
            guiGraphics.fill(0, 0, width, 1, resolveHudAccentColor(0xFFA855F7));
        }
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), (alpha << 24) | 0x00FFFFFF);
        endHudRender(guiGraphics);
    }

    private void registerReachSettings() {
        addSetting(new IntModuleSetting(
            "reach_fade_seconds",
            "Durée d'affichage",
            "Temps pendant lequel la reach reste visible.",
            "Reach",
            true,
            () -> fadeSeconds,
            value -> fadeSeconds = value,
            3,
            1,
            8,
            1,
            value -> value + " s"
        ));
        addSetting(new IntModuleSetting(
            "reach_decimals",
            "Décimales",
            "Nombre de décimales affichées.",
            "Reach",
            true,
            () -> decimals,
            value -> decimals = value,
            2,
            0,
            3,
            1,
            Integer::toString
        ));
        addSetting(new BooleanModuleSetting(
            "reach_show_unit",
            "Afficher l'unité",
            "Ajoute le suffixe blocks à la valeur.",
            "Reach",
            true,
            () -> showUnit,
            value -> showUnit = value,
            true
        ));
    }
}
