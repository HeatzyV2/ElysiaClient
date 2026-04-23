package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

public class AttackIndicator extends HudModule {
    private int barWidth;
    private int barHeight;
    private boolean showWhenReady;
    private boolean showPercent;

    public AttackIndicator() {
        super("AttackIndicator", "Affiche la barre de rechargement d'attaque.", 0, "⚔️");
        this.barWidth = 50;
        this.barHeight = 4;
        this.showWhenReady = false;
        this.showPercent = false;
        registerAttackSettings();
    }

    @Override
    public int getEditorWidth() {
        return barWidth + getHudPadding() * 2;
    }

    @Override
    public int getEditorHeight() {
        return getContentHeight();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        float progress = isPreviewRender() ? 0.72F : (mc.player != null ? mc.player.getAttackStrengthScale(0.0f) : 0.0F);
        if (!isPreviewRender() && mc.player == null) {
            return;
        }
        if (!showWhenReady && progress >= 1.0F && !isPreviewRender()) {
            return;
        }

        int contentHeight = getContentHeight();
        int frameWidth = barWidth + getHudPadding() * 2;
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, frameWidth, contentHeight, progress >= 1.0F ? 0xFF22C55E : 0xFFA855F7);

        int barX = getHudPadding();
        int barY = getHudPadding();
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, applyHudOpacity(0x55000000));
        guiGraphics.fill(barX, barY, barX + Math.round(barWidth * progress), barY + barHeight, resolveHudAccentColor(progress >= 1.0F ? 0xFF22C55E : 0xFFA855F7));

        if (showPercent) {
            String percent = Math.round(progress * 100.0F) + "%";
            drawHudText(guiGraphics, percent, getHudPadding(), barY + barHeight + 3, 0xFFFFFFFF);
        }

        endHudRender(guiGraphics);
    }

    private int getContentHeight() {
        return getHudPadding() * 2 + barHeight + (showPercent ? mc.font.lineHeight + 3 : 0);
    }

    private void registerAttackSettings() {
        addSetting(new IntModuleSetting(
            "attack_width",
            "Largeur",
            "Ajuste la largeur de la barre.",
            "Affichage",
            true,
            () -> barWidth,
            value -> barWidth = value,
            50,
            24,
            120,
            4,
            value -> value + " px"
        ));
        addSetting(new IntModuleSetting(
            "attack_height",
            "Hauteur",
            "Ajuste l'épaisseur de la barre.",
            "Affichage",
            true,
            () -> barHeight,
            value -> barHeight = value,
            4,
            3,
            10,
            1,
            value -> value + " px"
        ));
        addSetting(new BooleanModuleSetting(
            "attack_show_ready",
            "Afficher prêt",
            "Garde la barre visible même quand le coup est rechargé.",
            "Affichage",
            true,
            () -> showWhenReady,
            value -> showWhenReady = value,
            false
        ));
        addSetting(new BooleanModuleSetting(
            "attack_show_percent",
            "Afficher pourcentage",
            "Ajoute un pourcentage sous la barre.",
            "Affichage",
            true,
            () -> showPercent,
            value -> showPercent = value,
            false
        ));
    }
}
