package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;

public class ArmorWarningModule extends TextHudModule {
    private int warningThreshold;
    private int criticalThreshold;
    private boolean hideWhenSafe;

    public ArmorWarningModule() {
        super("ArmorWarning", "Indique si votre armure est faible.", "A!");
        this.warningThreshold = 12;
        this.criticalThreshold = 5;
        this.hideWhenSafe = false;
        registerWarningSettings();
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        int armor = mc.player.getArmorValue();
        if (armor <= criticalThreshold) return "Armor Critical";
        if (armor <= warningThreshold) return "Armor Low";
        return hideWhenSafe ? null : "Armor OK";
    }

    @Override
    protected int getAccentColor() {
        if (mc.player == null) return 0xFF64748B;
        int armor = mc.player.getArmorValue();
        if (armor <= criticalThreshold) return 0xFFEF4444;
        if (armor <= warningThreshold) return 0xFFF59E0B;
        return 0xFF22C55E;
    }

    private void registerWarningSettings() {
        addSetting(new IntModuleSetting(
            "armor_warning_threshold",
            "Seuil bas",
            "Définit le niveau d'armure considéré comme fragile.",
            "Alerte",
            true,
            () -> warningThreshold,
            value -> warningThreshold = Math.max(value, criticalThreshold + 1),
            12,
            4,
            20,
            1,
            value -> value + " armure"
        ));
        addSetting(new IntModuleSetting(
            "armor_critical_threshold",
            "Seuil critique",
            "Définit le niveau d'armure considéré comme dangereux.",
            "Alerte",
            true,
            () -> criticalThreshold,
            value -> criticalThreshold = Math.min(value, warningThreshold - 1),
            5,
            1,
            19,
            1,
            value -> value + " armure"
        ));
        addSetting(new BooleanModuleSetting(
            "armor_hide_safe",
            "Masquer si stable",
            "Cache le widget quand l'armure est confortable.",
            "Alerte",
            true,
            () -> hideWhenSafe,
            value -> hideWhenSafe = value,
            false
        ));
    }
}
