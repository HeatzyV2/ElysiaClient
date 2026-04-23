package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;

public class FoodWarningModule extends TextHudModule {
    private int warningThreshold;
    private int criticalThreshold;
    private boolean hideWhenSafe;

    public FoodWarningModule() {
        super("FoodWarning", "Indique quand il faut manger.", "EAT");
        this.warningThreshold = 12;
        this.criticalThreshold = 6;
        this.hideWhenSafe = false;
        registerWarningSettings();
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int food = mc.player.getFoodData().getFoodLevel();
        if (food <= criticalThreshold) return "Food Critical";
        if (food <= warningThreshold) return "Food Low";
        return hideWhenSafe ? null : "Food OK";
    }

    @Override
    protected int getAccentColor() {
        if (mc.player == null) return 0xFF64748B;
        int food = mc.player.getFoodData().getFoodLevel();
        if (food <= criticalThreshold) return 0xFFEF4444;
        if (food <= warningThreshold) return 0xFFF59E0B;
        return 0xFF22C55E;
    }

    private void registerWarningSettings() {
        addSetting(new IntModuleSetting(
            "food_warning_threshold",
            "Seuil bas",
            "Définit le niveau de faim considéré comme faible.",
            "Alerte",
            true,
            () -> warningThreshold,
            value -> warningThreshold = Math.max(value, criticalThreshold + 1),
            12,
            4,
            19,
            1,
            value -> value + " points"
        ));
        addSetting(new IntModuleSetting(
            "food_critical_threshold",
            "Seuil critique",
            "Définit le niveau de faim considéré comme critique.",
            "Alerte",
            true,
            () -> criticalThreshold,
            value -> criticalThreshold = Math.min(value, warningThreshold - 1),
            6,
            1,
            18,
            1,
            value -> value + " points"
        ));
        addSetting(new BooleanModuleSetting(
            "food_hide_safe",
            "Masquer si stable",
            "Cache le widget quand la faim est confortable.",
            "Alerte",
            true,
            () -> hideWhenSafe,
            value -> hideWhenSafe = value,
            false
        ));
    }
}
