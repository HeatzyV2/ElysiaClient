package net.elysiastudios.client.module.impl.smartfight;

import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.impl.SmartFightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class SmartFightHUDWidget implements HudWidget {
    private final SmartFightModule module;

    public SmartFightHUDWidget(SmartFightModule module) {
        this.module = module;
    }

    @Override
    public String getHudId() {
        return "smartfight-widget";
    }

    @Override
    public String getDisplayName() {
        return "SmartFight";
    }

    @Override
    public int getEditorWidth() {
        int base = module.getSmartFightSettings().compactMode ? 152 : 182;
        return Math.round(base * module.getSmartFightSettings().hudScale);
    }

    @Override
    public int getEditorHeight() {
        int base = module.getSmartFightSettings().compactMode ? 72 : 110;
        return Math.round(base * module.getSmartFightSettings().hudScale);
    }

    @Override
    public int getDefaultX() {
        return module.getMainWidgetDefaultX();
    }

    @Override
    public int getDefaultY() {
        return module.getMainWidgetDefaultY();
    }

    @Override
    public float getDefaultScale() {
        return 1.0F;
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
        FightStateSnapshot snapshot = module.getSnapshotForRender(preview);
        if (snapshot == null) {
            return;
        }

        if (!preview && !module.shouldRenderMainWidget()) {
            return;
        }

        SmartFightSettings settings = module.getSmartFightSettings();
        SmartFightRenderUtil.ThemePalette palette = SmartFightRenderUtil.palette(settings.themeMode);
        float alpha = settings.hudOpacity * (preview ? 0.96F : 1.0F);
        int width = settings.compactMode ? 152 : 182;
        int height = settings.compactMode ? 72 : 110;

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
            SmartFightRenderUtil.withAlpha(palette.glow(), alpha)
        );

        guiGraphics.fill(10, 10, width - 10, 11, SmartFightRenderUtil.withAlpha(palette.accentSoft(), alpha * 0.55F));
        guiGraphics.drawString(Minecraft.getInstance().font, "SmartFight", 12, 14, SmartFightRenderUtil.withAlpha(palette.text(), alpha), true);

        String modeChip = preview ? "PREVIEW" : module.isFightActive() ? "LIVE" : "IDLE";
        int chipWidth = Minecraft.getInstance().font.width(modeChip) + 10;
        SmartFightRenderUtil.drawRoundedPanel(
            guiGraphics,
            width - chipWidth - 12,
            12,
            chipWidth,
            14,
            7,
            SmartFightRenderUtil.withAlpha(palette.surface(), alpha),
            SmartFightRenderUtil.withAlpha(snapshot.accentColor(), alpha * 0.75F)
        );
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            modeChip,
            width - chipWidth - 7,
            16,
            SmartFightRenderUtil.withAlpha(snapshot.accentColor(), alpha),
            false
        );

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            SmartFightRenderUtil.trimToPixelWidth(snapshot.headline(), width - 24),
            12,
            32,
            SmartFightRenderUtil.withAlpha(snapshot.accentColor(), alpha),
            true
        );
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            SmartFightRenderUtil.trimToPixelWidth(snapshot.subline(), width - 24),
            12,
            44,
            SmartFightRenderUtil.withAlpha(palette.muted(), alpha),
            false
        );

        drawGauge(guiGraphics, "Ctrl", SmartFightRenderUtil.percent(snapshot.controlScore()), 12, settings.compactMode ? 57 : 58, 68, snapshot.controlScore(), palette, snapshot.accentColor(), alpha);
        drawGauge(guiGraphics, "Stable", SmartFightRenderUtil.percent(snapshot.stabilityScore()), width - 80, settings.compactMode ? 57 : 58, 68, snapshot.stabilityScore(), palette, palette.accentSoft(), alpha);

        List<Metric> metrics = buildMetrics(snapshot);
        if (settings.compactMode) {
            if (metrics.isEmpty()) {
                metrics.add(new Metric("Signal", "Minimal", palette.muted()));
            }
            drawCompactMetrics(guiGraphics, metrics, width, 66, palette, alpha);
        } else {
            int[] xs = {12, 92, 12, 92, 12};
            int[] ys = {76, 76, 87, 87, 98};
            int[] widths = {68, 78, 68, 78, width - 24};
            if (metrics.isEmpty()) {
                metrics.add(new Metric("Signal", "Analyse discrète", palette.muted()));
            }
            for (int i = 0; i < metrics.size() && i < xs.length; i++) {
                drawMetric(guiGraphics, metrics.get(i).label(), metrics.get(i).value(), xs[i], ys[i], widths[i], metrics.get(i).color(), palette, alpha);
            }
        }

        guiGraphics.pose().popMatrix();
        if (!preview) {
            renderCrosshairFeedback(guiGraphics, snapshot);
        }
    }

    private void drawGauge(
        GuiGraphics guiGraphics,
        String label,
        String value,
        int x,
        int y,
        int width,
        float progress,
        SmartFightRenderUtil.ThemePalette palette,
        int color,
        float alpha
    ) {
        guiGraphics.drawString(Minecraft.getInstance().font, label, x, y, SmartFightRenderUtil.withAlpha(palette.muted(), alpha), false);
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            value,
            x + width - Minecraft.getInstance().font.width(value),
            y,
            SmartFightRenderUtil.withAlpha(palette.text(), alpha),
            false
        );
        SmartFightRenderUtil.drawThinBar(
            guiGraphics,
            x,
            y + 9,
            width,
            3,
            progress,
            SmartFightRenderUtil.withAlpha(0x55223346, alpha),
            SmartFightRenderUtil.withAlpha(color, alpha)
        );
    }

    private void drawMetric(
        GuiGraphics guiGraphics,
        String label,
        String value,
        int x,
        int y,
        int width,
        int color,
        SmartFightRenderUtil.ThemePalette palette,
        float alpha
    ) {
        String labelText = label + ":";
        int labelWidth = Minecraft.getInstance().font.width(labelText);
        int availableWidth = Math.max(20, width - labelWidth - 6);
        String trimmedValue = SmartFightRenderUtil.trimToPixelWidth(value, availableWidth);

        guiGraphics.drawString(Minecraft.getInstance().font, labelText, x, y, SmartFightRenderUtil.withAlpha(palette.muted(), alpha), false);
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            trimmedValue,
            x + width - Minecraft.getInstance().font.width(trimmedValue),
            y,
            SmartFightRenderUtil.withAlpha(color, alpha),
            false
        );
    }

    private void drawCompactMetrics(
        GuiGraphics guiGraphics,
        List<Metric> metrics,
        int width,
        int y,
        SmartFightRenderUtil.ThemePalette palette,
        float alpha
    ) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < metrics.size() && i < 2; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(metrics.get(i).label()).append(": ").append(metrics.get(i).value());
        }

        String line = SmartFightRenderUtil.trimToPixelWidth(builder.toString(), width - 24);
        guiGraphics.drawString(Minecraft.getInstance().font, line, 12, y, SmartFightRenderUtil.withAlpha(palette.text(), alpha), false);
    }

    private void renderCrosshairFeedback(GuiGraphics guiGraphics, FightStateSnapshot snapshot) {
        SmartFightSettings settings = module.getSmartFightSettings();
        if (!module.isFightActive()) {
            return;
        }
        if (!shouldRenderTimingCrosshair(settings) && !shouldRenderSpacingCrosshair(settings)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        float alpha = settings.hudOpacity * settings.timingAssist.feedbackIntensity * 0.72F;
        int timingColor = SmartFightRenderUtil.withAlpha(snapshot.timingState().getColor(), alpha);
        int spacingColor = SmartFightRenderUtil.withAlpha(snapshot.spacingState().getColor(), alpha);
        int gap = 6;
        int length = 8;

        if (shouldRenderTimingCrosshair(settings)) {
            guiGraphics.fill(centerX - 1, centerY - gap - length, centerX + 1, centerY - gap, timingColor);
            guiGraphics.fill(centerX - 1, centerY + gap, centerX + 1, centerY + gap + length, timingColor);
        }
        if (shouldRenderSpacingCrosshair(settings)) {
            guiGraphics.fill(centerX - gap - length, centerY - 1, centerX - gap, centerY + 1, spacingColor);
            guiGraphics.fill(centerX + gap, centerY - 1, centerX + gap + length, centerY + 1, spacingColor);
        }
    }

    private boolean shouldRenderTimingCrosshair(SmartFightSettings settings) {
        return settings.timingAssist.enabled
            && (settings.timingAssist.displayMode == SmartFightSettings.DisplayMode.CROSSHAIR || settings.timingAssist.displayMode == SmartFightSettings.DisplayMode.BOTH);
    }

    private boolean shouldRenderSpacingCrosshair(SmartFightSettings settings) {
        return settings.reachSense.enabled
            && (settings.reachSense.displayMode == SmartFightSettings.DisplayMode.CROSSHAIR || settings.reachSense.displayMode == SmartFightSettings.DisplayMode.BOTH);
    }

    private List<Metric> buildMetrics(FightStateSnapshot snapshot) {
        SmartFightSettings settings = module.getSmartFightSettings();
        List<Metric> metrics = new ArrayList<>();

        if (settings.timingAssist.enabled
            && (settings.timingAssist.displayMode == SmartFightSettings.DisplayMode.WIDGET || settings.timingAssist.displayMode == SmartFightSettings.DisplayMode.BOTH)) {
            metrics.add(new Metric("Timing", snapshot.timingState().getLabel(), snapshot.timingState().getColor()));
        }
        if (settings.reachSense.enabled
            && (settings.reachSense.displayMode == SmartFightSettings.DisplayMode.WIDGET || settings.reachSense.displayMode == SmartFightSettings.DisplayMode.BOTH)) {
            metrics.add(new Metric("Spacing", snapshot.spacingState().getLabel(), snapshot.spacingState().getColor()));
        }
        if (settings.rhythmTracker.enabled && settings.rhythmTracker.displayMode == SmartFightSettings.RhythmDisplayMode.WIDGET) {
            metrics.add(new Metric("Rythme", snapshot.rhythmState().getLabel(), snapshot.rhythmState().getColor()));
        }
        if (settings.pressureAnalyzer.enabled) {
            metrics.add(new Metric("Press.", snapshot.pressureState().getLabel(), snapshot.pressureState().getColor()));
        }
        if (settings.opponentPattern.enabled && settings.opponentPattern.showEstimatedPattern) {
            String label = snapshot.confidence() >= settings.opponentPattern.confidenceThreshold
                ? snapshot.opponentPattern().getLabel()
                : "Lecture...";
            int color = snapshot.confidence() >= settings.opponentPattern.confidenceThreshold
                ? snapshot.opponentPattern().getColor()
                : 0xFF94A3B8;
            metrics.add(new Metric("Adverse", label, color));
        }

        return metrics;
    }

    private record Metric(String label, String value, int color) {
    }
}
