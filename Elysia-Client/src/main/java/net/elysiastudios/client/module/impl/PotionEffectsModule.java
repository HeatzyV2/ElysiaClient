package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionEffectsModule extends HudModule {
    private boolean showBeneficial;
    private boolean showNegative;
    private boolean showDuration;

    public PotionEffectsModule() {
        super("PotionEffects", "Affiche les effets de potion actifs", 0, "🧪");
        this.showBeneficial = true;
        this.showNegative = true;
        this.showDuration = true;
        registerPotionSettings();
    }

    @Override
    public int getEditorWidth() {
        return 140;
    }

    @Override
    public int getEditorHeight() {
        return Math.max(32, getRowsForRender().size() * 16);
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        List<EffectRow> rows = getRowsForRender();
        if (rows.isEmpty()) {
            return;
        }

        int width = 0;
        for (EffectRow row : rows) {
            width = Math.max(width, measureHudTextWidth(row.text()));
        }
        int height = rows.size() * 16;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF22C55E);
        int y = 0;
        for (EffectRow row : rows) {
            if (y > 0 && shouldShowHudBackground()) {
                guiGraphics.fill(0, y, width, y + 1, applyHudOpacity(0x22000000));
            }
            drawHudText(guiGraphics, row.text(), getHudPadding(), y + 4, row.color());
            y += 16;
        }
        endHudRender(guiGraphics);
    }

    private List<EffectRow> getRowsForRender() {
        List<EffectRow> rows = new ArrayList<>();
        if (isPreviewRender()) {
            rows.add(new EffectRow("Speed II 1:32", 0xFF22C55E));
            rows.add(new EffectRow("Weakness 0:18", 0xFFEF4444));
            return rows;
        }
        if (mc.player == null) {
            return rows;
        }

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        for (MobEffectInstance effect : effects) {
            boolean beneficial = effect.getEffect().value().isBeneficial();
            if ((beneficial && !showBeneficial) || (!beneficial && !showNegative)) {
                continue;
            }

            String name = effect.getEffect().value().getDescriptionId();
            String simpleName = name.substring(name.lastIndexOf('.') + 1);
            simpleName = simpleName.substring(0, 1).toUpperCase() + simpleName.substring(1);
            String amp = effect.getAmplifier() > 0 ? " " + toRoman(effect.getAmplifier() + 1) : "";
            String suffix = "";
            if (showDuration) {
                int seconds = effect.getDuration() / 20;
                suffix = " " + (seconds / 60) + ":" + String.format("%02d", seconds % 60);
            }
            rows.add(new EffectRow(simpleName + amp + suffix, beneficial ? 0xFF22C55E : 0xFFEF4444));
        }
        return rows;
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    private void registerPotionSettings() {
        addSetting(new BooleanModuleSetting(
            "potion_show_positive",
            "Afficher positifs",
            "Affiche les effets bénéfiques.",
            "Potions",
            true,
            () -> showBeneficial,
            value -> showBeneficial = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "potion_show_negative",
            "Afficher négatifs",
            "Affiche les effets négatifs.",
            "Potions",
            true,
            () -> showNegative,
            value -> showNegative = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "potion_show_duration",
            "Afficher durée",
            "Ajoute la durée restante après le nom de l'effet.",
            "Potions",
            true,
            () -> showDuration,
            value -> showDuration = value,
            true
        ));
    }

    private record EffectRow(String text, int color) {}
}
