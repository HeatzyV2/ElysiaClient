package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.module.ModuleManager;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

public class ComboDisplayModule extends HudModule {
    private static int comboCount;
    private static long lastHitTime;

    private int fadeSeconds;
    private int resetWindowMillis;
    private boolean showLabel;

    public ComboDisplayModule() {
        super("ComboDisplay", "Affiche les combos consécutifs", 0, "🔥");
        this.fadeSeconds = 3;
        this.resetWindowMillis = 2000;
        this.showLabel = true;
        registerComboSettings();
    }

    public static void onHit() {
        long now = System.currentTimeMillis();
        ComboDisplayModule module = ModuleManager.getInstance().getModule(ComboDisplayModule.class);
        int resetWindow = module != null ? module.resetWindowMillis : 2000;
        if (now - lastHitTime > resetWindow) {
            comboCount = 0;
        }
        comboCount++;
        lastHitTime = now;
    }

    public static void onHurt() {
        comboCount = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        long visibleWindow = fadeSeconds * 1000L;
        long elapsed = isPreviewRender() ? 500L : System.currentTimeMillis() - lastHitTime;
        int count = isPreviewRender() ? 6 : comboCount;
        if (count <= 0 || elapsed > visibleWindow) {
            return;
        }

        String text = showLabel ? count + " Combo" : Integer.toString(count);
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int alpha = (int) (Math.max(0.0D, 1.0D - (elapsed / (double) visibleWindow)) * 255.0D);
        if (alpha <= 10) {
            return;
        }

        int accent = count >= 10 ? 0xFFEF4444 : count >= 5 ? 0xFFF59E0B : 0xFFA855F7;
        beginHudRender(guiGraphics);
        if (shouldShowHudBackground()) {
            guiGraphics.fill(0, 0, width, height, applyHudOpacity((alpha << 24) | 0x00101018));
        }
        if (shouldShowHudAccent()) {
            guiGraphics.fill(0, 0, width, 1, resolveHudAccentColor(accent));
        }
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), (alpha << 24) | 0x00FFFFFF);
        endHudRender(guiGraphics);
    }

    private void registerComboSettings() {
        addSetting(new IntModuleSetting(
            "combo_fade_seconds",
            "Durée d'affichage",
            "Temps pendant lequel le combo reste visible.",
            "Combo",
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
            "combo_reset_window",
            "Fenêtre de reset",
            "Délai maximal entre deux hits pour conserver le combo.",
            "Combo",
            true,
            () -> resetWindowMillis,
            value -> resetWindowMillis = value,
            2000,
            500,
            5000,
            250,
            value -> value + " ms"
        ));
        addSetting(new BooleanModuleSetting(
            "combo_show_label",
            "Afficher le label",
            "Ajoute le mot Combo à côté du compteur.",
            "Combo",
            true,
            () -> showLabel,
            value -> showLabel = value,
            true
        ));
    }
}
