package net.elysiastudios.client.module.impl.smartfight;

import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.impl.SmartFightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class FightSummaryWidget implements HudWidget {
    private final SmartFightModule module;

    public FightSummaryWidget(SmartFightModule module) {
        this.module = module;
    }

    @Override
    public String getHudId() {
        return "smartfight-summary";
    }

    @Override
    public String getDisplayName() {
        return "Résumé SmartFight";
    }

    @Override
    public int getEditorWidth() {
        return Math.round(176 * module.getSmartFightSettings().hudScale);
    }

    @Override
    public int getEditorHeight() {
        int base = 70;
        SmartFightSettings.FightSummarySettings summary = module.getSmartFightSettings().fightSummary;
        if (summary.showDiagnosis) {
            base += 10;
        }
        if (summary.showPressure) {
            base += 10;
        }
        return Math.round(base * module.getSmartFightSettings().hudScale);
    }

    @Override
    public int getDefaultX() {
        return module.getMainWidgetDefaultX();
    }

    @Override
    public int getDefaultY() {
        return module.getRecommendedSummaryY();
    }

    @Override
    public boolean allowMoveInHudEditor() {
        return module.getSmartFightSettings().allowMoveInHudEditor;
    }

    @Override
    public boolean shouldRenderInHudEditor() {
        return module.getSmartFightSettings().previewInHudEditor;
    }

    @Override
    public void render(GuiGraphics guiGraphics, boolean preview) {
        FightSummary summary = module.getSummaryForRender(preview);
        if (summary == null || (!preview && !module.shouldRenderSummaryWidget())) {
            return;
        }

        SmartFightSettings settings = module.getSmartFightSettings();
        SmartFightSettings.FightSummarySettings summarySettings = settings.fightSummary;
        SmartFightRenderUtil.ThemePalette palette = SmartFightRenderUtil.palette(settings.themeMode);
        float alpha = settings.hudOpacity * (preview ? 0.94F : 0.96F);
        int width = 176;
        int height = getEditorHeight() <= 0 ? 72 : Math.round(getEditorHeight() / settings.hudScale);
        int contentWidth = width - 24;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) getX(), (float) getY());
        guiGraphics.pose().scale(getScale() * settings.hudScale, getScale() * settings.hudScale);

        SmartFightRenderUtil.drawRoundedPanel(
            guiGraphics,
            0,
            0,
            width,
            height,
            12,
            SmartFightRenderUtil.withAlpha(palette.background(), alpha),
            SmartFightRenderUtil.withAlpha(summary.interpretation().getColor(), alpha * 0.7F)
        );

        guiGraphics.drawString(Minecraft.getInstance().font, "Résumé SmartFight", 12, 12, SmartFightRenderUtil.withAlpha(palette.text(), alpha), true);
        String duration = SmartFightRenderUtil.duration(summary.durationMillis());
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            duration,
            width - 12 - Minecraft.getInstance().font.width(duration),
            12,
            SmartFightRenderUtil.withAlpha(palette.muted(), alpha),
            false
        );

        int lineY = 28;
        if (summarySettings.showDiagnosis) {
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                SmartFightRenderUtil.trimToPixelWidth(summary.diagnosis(), contentWidth),
                12,
                lineY,
                SmartFightRenderUtil.withAlpha(summary.interpretation().getColor(), alpha),
                false
            );
            lineY += 12;
        }

        if (summarySettings.showAccuracy) {
            drawLine(guiGraphics, "Précision", SmartFightRenderUtil.percent(summary.accuracy()), 12, lineY, contentWidth, palette, alpha);
            lineY += 10;
        }
        if (summarySettings.showSpacing) {
            drawLine(guiGraphics, "Spacing", summary.spacingState().getLabel() + " - " + SmartFightRenderUtil.distance(summary.averageDistance()), 12, lineY, contentWidth, palette, alpha);
            lineY += 10;
        }
        if (summarySettings.showRhythm) {
            drawLine(guiGraphics, "Rythme", summary.rhythmState().getLabel(), 12, lineY, contentWidth, palette, alpha);
            lineY += 10;
        }
        if (summarySettings.showPressure) {
            drawLine(guiGraphics, "Pression", summary.pressureState().getLabel(), 12, lineY, contentWidth, palette, alpha);
            lineY += 10;
        }

        drawLine(guiGraphics, "Stabilité", SmartFightRenderUtil.percent(summary.stabilityScore()), 12, height - 14, contentWidth, palette, alpha);
        guiGraphics.pose().popMatrix();
    }

    private void drawLine(
        GuiGraphics guiGraphics,
        String label,
        String value,
        int x,
        int y,
        int width,
        SmartFightRenderUtil.ThemePalette palette,
        float alpha
    ) {
        String labelText = label + ":";
        int labelWidth = Minecraft.getInstance().font.width(labelText);
        int availableWidth = Math.max(24, width - labelWidth - 6);
        String trimmedValue = SmartFightRenderUtil.trimToPixelWidth(value, availableWidth);

        guiGraphics.drawString(Minecraft.getInstance().font, labelText, x, y, SmartFightRenderUtil.withAlpha(palette.muted(), alpha), false);
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            trimmedValue,
            x + width - Minecraft.getInstance().font.width(trimmedValue),
            y,
            SmartFightRenderUtil.withAlpha(palette.text(), alpha),
            false
        );
    }
}
