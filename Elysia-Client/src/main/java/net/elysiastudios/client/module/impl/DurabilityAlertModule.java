package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class DurabilityAlertModule extends TextHudModule {
    private int lastLowest = 101;
    private int warningThreshold;
    private int criticalThreshold;
    private boolean includeHands;
    private boolean hideWhenSafe;

    public DurabilityAlertModule() {
        super("DurabilityAlert", "Affiche la durabilité la plus basse équipée.", "DURA");
        this.warningThreshold = 50;
        this.criticalThreshold = 25;
        this.includeHands = true;
        this.hideWhenSafe = false;
        registerDurabilitySettings();
    }

    @Override
    protected String getText() {
        if (mc.player == null) return null;
        lastLowest = 101;
        if (includeHands) {
            lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getMainHandItem()));
            lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getOffhandItem()));
        }
        lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getItemBySlot(EquipmentSlot.HEAD)));
        lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getItemBySlot(EquipmentSlot.CHEST)));
        lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getItemBySlot(EquipmentSlot.LEGS)));
        lastLowest = Math.min(lastLowest, durabilityPercent(mc.player.getItemBySlot(EquipmentSlot.FEET)));

        if (lastLowest > 100) {
            return hideWhenSafe ? null : "Durability OK";
        }
        if (hideWhenSafe && lastLowest > warningThreshold) {
            return null;
        }
        return "Lowest Dura " + lastLowest + "%";
    }

    @Override
    protected int getAccentColor() {
        if (lastLowest <= criticalThreshold) return 0xFFEF4444;
        if (lastLowest <= warningThreshold) return 0xFFF59E0B;
        return 0xFF22C55E;
    }

    private int durabilityPercent(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return 101;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return Math.round(remaining * 100.0F / stack.getMaxDamage());
    }

    private void registerDurabilitySettings() {
        addSetting(new IntModuleSetting(
            "dura_warning_threshold",
            "Seuil bas",
            "Définit le pourcentage considéré comme fragile.",
            "Alerte",
            true,
            () -> warningThreshold,
            value -> warningThreshold = Math.max(value, criticalThreshold + 1),
            50,
            10,
            90,
            5,
            value -> value + "%"
        ));
        addSetting(new IntModuleSetting(
            "dura_critical_threshold",
            "Seuil critique",
            "Définit le pourcentage considéré comme critique.",
            "Alerte",
            true,
            () -> criticalThreshold,
            value -> criticalThreshold = Math.min(value, warningThreshold - 1),
            25,
            5,
            80,
            5,
            value -> value + "%"
        ));
        addSetting(new BooleanModuleSetting(
            "dura_include_hands",
            "Inclure les mains",
            "Prend aussi en compte l'arme et l'offhand.",
            "Alerte",
            true,
            () -> includeHands,
            value -> includeHands = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "dura_hide_safe",
            "Masquer si stable",
            "Cache le widget quand tout l'équipement est sain.",
            "Alerte",
            true,
            () -> hideWhenSafe,
            value -> hideWhenSafe = value,
            false
        ));
    }
}
