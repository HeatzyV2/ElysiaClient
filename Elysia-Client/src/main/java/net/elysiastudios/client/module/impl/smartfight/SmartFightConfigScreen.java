package net.elysiastudios.client.module.impl.smartfight;

import net.elysiastudios.client.gui.HudEditorScreen;
import net.elysiastudios.client.module.impl.SmartFightModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class SmartFightConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 32;
    private static final int ROW_GAP = 8;

    private final Screen parent;
    private final SmartFightModule module;
    private int selectedPage;
    private double scrollAmount;

    public SmartFightConfigScreen(Screen parent, SmartFightModule module) {
        super(Component.literal("SmartFight"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xC0080B12);

        int panelWidth = Math.min(width - 36, 760);
        int panelHeight = Math.min(height - 36, 470);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        SmartFightRenderUtil.ThemePalette palette = SmartFightRenderUtil.palette(module.getSmartFightSettings().themeMode);

        SmartFightRenderUtil.drawRoundedPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, 16, 0xF010131B, palette.glow());
        guiGraphics.drawString(font, "SmartFight", panelX + 20, panelY + 16, palette.text(), true);
        guiGraphics.drawString(font, SmartFightRenderUtil.trimToPixelWidth("Configuration premium, discrète et orientée lecture du fight", Math.max(80, panelWidth - 200)), panelX + 20, panelY + 31, palette.muted(), false);

        int hudX = panelX + panelWidth - 150;
        int hudY = panelY + 14;
        boolean hudHovered = inside(mouseX, mouseY, hudX, hudY, 118, 22);
        SmartFightRenderUtil.drawRoundedPanel(guiGraphics, hudX, hudY, 118, 22, 10, hudHovered ? 0xFF1D4ED8 : 0xCC1E3A8A, palette.accentSoft());
        guiGraphics.drawCenteredString(font, "Éditeur HUD", hudX + 59, hudY + 7, 0xFFFFFFFF);

        int tabsY = panelY + 54;
        int tabX = panelX + 20;
        int tabGap = 8;
        int maxTabWidth = Math.max(58, (panelWidth - 40 - ((ConfigPage.values().length - 1) * tabGap)) / ConfigPage.values().length);
        for (int i = 0; i < ConfigPage.values().length; i++) {
            ConfigPage page = ConfigPage.values()[i];
            boolean selected = i == selectedPage;
            int tabWidth = Math.min(font.width(page.title) + 22, maxTabWidth);
            SmartFightRenderUtil.drawRoundedPanel(
                guiGraphics,
                tabX,
                tabsY,
                tabWidth,
                20,
                8,
                selected ? palette.surface() : 0x440F172A,
                selected ? palette.accent() : 0x33233446
            );
            guiGraphics.drawString(font, SmartFightRenderUtil.trimToPixelWidth(page.title, tabWidth - 14), tabX + 8, tabsY + 6, selected ? palette.text() : palette.muted(), false);
            tabX += tabWidth + tabGap;
        }

        int contentX = panelX + 20;
        int contentY = panelY + 88;
        int rowWidth = panelWidth - 52;
        int contentHeight = Math.max(92, panelHeight - 138);

        List<ConfigEntry> entries = buildEntries();
        int totalHeight = entries.isEmpty() ? 0 : entries.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        int maxScroll = Math.max(0, totalHeight - contentHeight);
        scrollAmount = Math.max(0.0D, Math.min(scrollAmount, maxScroll));

        ConfigEntry hoveredEntry = null;
        guiGraphics.enableScissor(contentX, contentY, contentX + rowWidth, contentY + contentHeight);

        int drawY = contentY - (int) scrollAmount;
        for (ConfigEntry entry : entries) {
            boolean hovered = inside(mouseX, mouseY, contentX, drawY, rowWidth, ROW_HEIGHT);
            if (hovered) {
                hoveredEntry = entry;
            }

            SmartFightRenderUtil.drawRoundedPanel(
                guiGraphics,
                contentX,
                drawY,
                rowWidth,
                ROW_HEIGHT,
                10,
                hovered ? 0xFF161E2B : 0xCC0F1720,
                hovered ? palette.accentSoft() : 0x22233446
            );

            String rawValue = entry.value.get();
            int valueWidth = Math.min(Math.max(72, font.width(rawValue) + 16), Math.max(96, rowWidth / 3));
            int valueX = contentX + rowWidth - valueWidth - 10;
            SmartFightRenderUtil.drawRoundedPanel(
                guiGraphics,
                valueX,
                drawY + 7,
                valueWidth,
                18,
                8,
                hovered ? 0xFF1A2432 : 0xAA131B27,
                hovered ? palette.accent() : palette.accentSoft()
            );

            String label = SmartFightRenderUtil.trimToPixelWidth(entry.label, Math.max(40, valueX - contentX - 24));
            String value = SmartFightRenderUtil.trimToPixelWidth(rawValue, valueWidth - 12);
            guiGraphics.drawString(font, label, contentX + 12, drawY + 11, palette.text(), false);
            guiGraphics.drawString(font, value, valueX + 6, drawY + 12, hovered ? palette.accent() : palette.muted(), false);

            drawY += ROW_HEIGHT + ROW_GAP;
        }

        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 10;
            int thumbHeight = Math.max(28, (int) ((contentHeight / (double) totalHeight) * contentHeight));
            int thumbY = contentY + (int) ((scrollAmount / maxScroll) * (contentHeight - thumbHeight));
            guiGraphics.fill(scrollbarX, contentY, scrollbarX + 4, contentY + contentHeight, 0x221E293B);
            SmartFightRenderUtil.fillRoundedRect(guiGraphics, scrollbarX, thumbY, 4, thumbHeight, 2, palette.accent());
        }

        String hint = hoveredEntry != null ? hoveredEntry.hint : "Clic gauche = avancer. Clic droit = revenir.";
        guiGraphics.drawString(font, SmartFightRenderUtil.trimToPixelWidth(hint, panelWidth - 40), panelX + 20, panelY + panelHeight - 32, palette.muted(), false);
        guiGraphics.drawString(
            font,
            SmartFightRenderUtil.trimToPixelWidth("Échap ou Retour pour fermer. Les positions restent gérées via l'éditeur HUD.", panelWidth - 40),
            panelX + 20,
            panelY + panelHeight - 18,
            0xFF7C8CA3,
            false
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int panelWidth = Math.min(width - 36, 760);
        int panelHeight = Math.min(height - 36, 470);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        int hudX = panelX + panelWidth - 150;
        int hudY = panelY + 14;
        if (inside(mouseX, mouseY, hudX, hudY, 118, 22) && button == 0) {
            module.saveSettings();
            minecraft.setScreen(new HudEditorScreen(this));
            return true;
        }

        int tabsY = panelY + 54;
        int tabX = panelX + 20;
        int tabGap = 8;
        int maxTabWidth = Math.max(58, (panelWidth - 40 - ((ConfigPage.values().length - 1) * tabGap)) / ConfigPage.values().length);
        for (int i = 0; i < ConfigPage.values().length; i++) {
            int tabWidth = Math.min(font.width(ConfigPage.values()[i].title) + 22, maxTabWidth);
            if (inside(mouseX, mouseY, tabX, tabsY, tabWidth, 20)) {
                selectedPage = i;
                scrollAmount = 0.0D;
                return true;
            }
            tabX += tabWidth + tabGap;
        }

        int contentX = panelX + 20;
        int contentY = panelY + 88;
        int rowWidth = panelWidth - 52;
        int contentHeight = Math.max(92, panelHeight - 138);

        if (!inside(mouseX, mouseY, contentX, contentY, rowWidth, contentHeight)) {
            return super.mouseClicked(event, doubleClick);
        }

        List<ConfigEntry> entries = buildEntries();
        int drawY = contentY - (int) scrollAmount;
        for (ConfigEntry entry : entries) {
            if (inside(mouseX, mouseY, contentX, drawY, rowWidth, ROW_HEIGHT)) {
                if (button == 1) {
                    entry.backward.run();
                } else {
                    entry.forward.run();
                }
                module.saveSettings();
                return true;
            }
            drawY += ROW_HEIGHT + ROW_GAP;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelWidth = Math.min(width - 36, 760);
        int panelHeight = Math.min(height - 36, 470);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int contentX = panelX + 20;
        int contentY = panelY + 88;
        int contentWidth = panelWidth - 52;
        int contentHeight = Math.max(92, panelHeight - 138);

        if (!inside(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int totalHeight = Math.max(0, buildEntries().size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP);
        int maxScroll = Math.max(0, totalHeight - contentHeight);
        if (maxScroll <= 0) {
            return true;
        }

        scrollAmount = Math.max(0.0D, Math.min(scrollAmount - verticalAmount * 24.0D, maxScroll));
        return true;
    }

    @Override
    public void onClose() {
        module.saveSettings();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<ConfigEntry> buildEntries() {
        selectedPage = Math.max(0, Math.min(selectedPage, ConfigPage.values().length - 1));

        SmartFightSettings settings = module.getSmartFightSettings();
        List<ConfigEntry> entries = new ArrayList<>();

        switch (ConfigPage.values()[selectedPage]) {
            case GLOBAL -> {
                entries.add(entry("Mode combat", () -> settings.combatMode.getLabel(), () -> settings.combatMode = cycle(settings.combatMode, 1), () -> settings.combatMode = cycle(settings.combatMode, -1), "Java cooldown, Crystal PvP, Mace ou PvP 1.8 selon le style visé."));
                entries.add(entry("Mode d'analyse", () -> settings.analysisMode.getLabel(), () -> settings.analysisMode = cycle(settings.analysisMode, 1), () -> settings.analysisMode = cycle(settings.analysisMode, -1), "Normal ou Advanced selon la finesse de lecture."));
                entries.add(entry("Timeout combat", () -> settings.combatTimeoutMs + " ms", () -> settings.combatTimeoutMs += 200, () -> settings.combatTimeoutMs -= 200, "Délai avant fermeture de session après inactivité."));
                entries.add(entry("Afficher hors combat", () -> bool(!settings.showOnlyInCombat), () -> settings.showOnlyInCombat = !settings.showOnlyInCombat, () -> settings.showOnlyInCombat = !settings.showOnlyInCombat, "Laisse le widget visible même hors échange."));
                entries.add(entry("Échelle HUD", () -> formatFloat(settings.hudScale), () -> settings.hudScale += 0.05F, () -> settings.hudScale -= 0.05F, "Taille globale du rendu SmartFight."));
                entries.add(entry("Opacité HUD", () -> formatFloat(settings.hudOpacity), () -> settings.hudOpacity += 0.05F, () -> settings.hudOpacity -= 0.05F, "Niveau de discrétion visuelle du module."));
                entries.add(entry("Mode compact", () -> bool(settings.compactMode), () -> settings.compactMode = !settings.compactMode, () -> settings.compactMode = !settings.compactMode, "Réduit la hauteur du widget principal."));
                entries.add(entry("Animations", () -> bool(settings.animations), () -> settings.animations = !settings.animations, () -> settings.animations = !settings.animations, "Lissage des transitions et des états."));
                entries.add(entry("Thème", () -> settings.themeMode.getLabel(), () -> settings.themeMode = cycle(settings.themeMode, 1), () -> settings.themeMode = cycle(settings.themeMode, -1), "Palette générale du widget."));
                entries.add(entry("Preview éditeur HUD", () -> bool(settings.previewInHudEditor), () -> settings.previewInHudEditor = !settings.previewInHudEditor, () -> settings.previewInHudEditor = !settings.previewInHudEditor, "Affiche SmartFight en démonstration dans l'éditeur HUD."));
            }
            case TIMING_REACH -> {
                entries.add(entry("Timing actif", () -> bool(settings.timingAssist.enabled), () -> settings.timingAssist.enabled = !settings.timingAssist.enabled, () -> settings.timingAssist.enabled = !settings.timingAssist.enabled, "Active l'analyse de timing."));
                entries.add(entry("Timing display", () -> settings.timingAssist.displayMode.getLabel(), () -> settings.timingAssist.displayMode = cycle(settings.timingAssist.displayMode, 1), () -> settings.timingAssist.displayMode = cycle(settings.timingAssist.displayMode, -1), "Widget, crosshair, les deux ou off."));
                entries.add(entry("Timing intensité", () -> formatFloat(settings.timingAssist.feedbackIntensity), () -> settings.timingAssist.feedbackIntensity += 0.05F, () -> settings.timingAssist.feedbackIntensity -= 0.05F, "Intensité du feedback discret."));
                entries.add(entry("Timing smoothing", () -> formatFloat(settings.timingAssist.smoothing), () -> settings.timingAssist.smoothing += 0.05F, () -> settings.timingAssist.smoothing -= 0.05F, "Lissage du ressenti timing."));
                entries.add(entry("Reach actif", () -> bool(settings.reachSense.enabled), () -> settings.reachSense.enabled = !settings.reachSense.enabled, () -> settings.reachSense.enabled = !settings.reachSense.enabled, "Active la lecture de spacing."));
                entries.add(entry("Reach display", () -> settings.reachSense.displayMode.getLabel(), () -> settings.reachSense.displayMode = cycle(settings.reachSense.displayMode, 1), () -> settings.reachSense.displayMode = cycle(settings.reachSense.displayMode, -1), "Widget, crosshair, les deux ou off."));
                entries.add(entry("État distance", () -> bool(settings.reachSense.showDistanceState), () -> settings.reachSense.showDistanceState = !settings.reachSense.showDistanceState, () -> settings.reachSense.showDistanceState = !settings.reachSense.showDistanceState, "Affiche la distance moyenne dans le résumé."));
                entries.add(entry("Reach smoothing", () -> formatFloat(settings.reachSense.smoothing), () -> settings.reachSense.smoothing += 0.05F, () -> settings.reachSense.smoothing -= 0.05F, "Lissage du spacing."));
            }
            case RHYTHM_PATTERN -> {
                entries.add(entry("Rythme actif", () -> bool(settings.rhythmTracker.enabled), () -> settings.rhythmTracker.enabled = !settings.rhythmTracker.enabled, () -> settings.rhythmTracker.enabled = !settings.rhythmTracker.enabled, "Active le suivi du tempo."));
                entries.add(entry("Rythme display", () -> settings.rhythmTracker.displayMode.getLabel(), () -> settings.rhythmTracker.displayMode = cycle(settings.rhythmTracker.displayMode, 1), () -> settings.rhythmTracker.displayMode = cycle(settings.rhythmTracker.displayMode, -1), "Widget, icône ou off."));
                entries.add(entry("Détail rythme", () -> settings.rhythmTracker.detailLevel.getLabel(), () -> settings.rhythmTracker.detailLevel = cycle(settings.rhythmTracker.detailLevel, 1), () -> settings.rhythmTracker.detailLevel = cycle(settings.rhythmTracker.detailLevel, -1), "Simple ou plus détaillé."));
                entries.add(entry("Pression active", () -> bool(settings.pressureAnalyzer.enabled), () -> settings.pressureAnalyzer.enabled = !settings.pressureAnalyzer.enabled, () -> settings.pressureAnalyzer.enabled = !settings.pressureAnalyzer.enabled, "Active l'estimation de pression."));
                entries.add(entry("Détail pression", () -> settings.pressureAnalyzer.detailLevel.getLabel(), () -> settings.pressureAnalyzer.detailLevel = cycle(settings.pressureAnalyzer.detailLevel, 1), () -> settings.pressureAnalyzer.detailLevel = cycle(settings.pressureAnalyzer.detailLevel, -1), "Simple ou Advanced."));
                entries.add(entry("Pattern adverse", () -> bool(settings.opponentPattern.enabled), () -> settings.opponentPattern.enabled = !settings.opponentPattern.enabled, () -> settings.opponentPattern.enabled = !settings.opponentPattern.enabled, "Active l'estimation du style adverse."));
                entries.add(entry("Afficher pattern", () -> bool(settings.opponentPattern.showEstimatedPattern), () -> settings.opponentPattern.showEstimatedPattern = !settings.opponentPattern.showEstimatedPattern, () -> settings.opponentPattern.showEstimatedPattern = !settings.opponentPattern.showEstimatedPattern, "Affiche la tendance adverse dans le widget."));
                entries.add(entry("Seuil confiance", () -> formatFloat(settings.opponentPattern.confidenceThreshold), () -> settings.opponentPattern.confidenceThreshold += 0.05F, () -> settings.opponentPattern.confidenceThreshold -= 0.05F, "Filtre d'affichage des patterns trop fragiles."));
            }
            case SUMMARY_HUD -> {
                entries.add(entry("Résumé actif", () -> bool(settings.fightSummary.enabled), () -> settings.fightSummary.enabled = !settings.fightSummary.enabled, () -> settings.fightSummary.enabled = !settings.fightSummary.enabled, "Affiche le diagnostic à la fin du fight."));
                entries.add(entry("Durée résumé", () -> settings.fightSummary.durationSeconds + " s", () -> settings.fightSummary.durationSeconds += 1, () -> settings.fightSummary.durationSeconds -= 1, "Temps d'affichage du résumé post-combat."));
                entries.add(entry("Diag. résumé", () -> bool(settings.fightSummary.showDiagnosis), () -> settings.fightSummary.showDiagnosis = !settings.fightSummary.showDiagnosis, () -> settings.fightSummary.showDiagnosis = !settings.fightSummary.showDiagnosis, "Affiche la conclusion synthétique."));
                entries.add(entry("Précision", () -> bool(settings.fightSummary.showAccuracy), () -> settings.fightSummary.showAccuracy = !settings.fightSummary.showAccuracy, () -> settings.fightSummary.showAccuracy = !settings.fightSummary.showAccuracy, "Affiche le taux de précision."));
                entries.add(entry("Spacing", () -> bool(settings.fightSummary.showSpacing), () -> settings.fightSummary.showSpacing = !settings.fightSummary.showSpacing, () -> settings.fightSummary.showSpacing = !settings.fightSummary.showSpacing, "Affiche la lecture de distance."));
                entries.add(entry("Rythme", () -> bool(settings.fightSummary.showRhythm), () -> settings.fightSummary.showRhythm = !settings.fightSummary.showRhythm, () -> settings.fightSummary.showRhythm = !settings.fightSummary.showRhythm, "Affiche la tenue du tempo."));
                entries.add(entry("Pression", () -> bool(settings.fightSummary.showPressure), () -> settings.fightSummary.showPressure = !settings.fightSummary.showPressure, () -> settings.fightSummary.showPressure = !settings.fightSummary.showPressure, "Affiche l'état de pression final."));
                entries.add(entry("Déplaçable HUD", () -> bool(settings.allowMoveInHudEditor), () -> settings.allowMoveInHudEditor = !settings.allowMoveInHudEditor, () -> settings.allowMoveInHudEditor = !settings.allowMoveInHudEditor, "Autorise le déplacement des widgets dans l'éditeur HUD."));
            }
        }

        settings.sanitize();
        return entries;
    }

    private static ConfigEntry entry(String label, Supplier<String> value, Runnable forward, Runnable backward, String hint) {
        return new ConfigEntry(label, value, forward, backward, hint);
    }

    private static String bool(boolean value) {
        return value ? "Oui" : "Non";
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static <E extends Enum<E>> E cycle(E current, int direction) {
        E[] values = current.getDeclaringClass().getEnumConstants();
        int index = current.ordinal() + direction;
        if (index < 0) {
            index = values.length - 1;
        } else if (index >= values.length) {
            index = 0;
        }
        return values[index];
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private enum ConfigPage {
        GLOBAL("Global"),
        TIMING_REACH("Timing / Reach"),
        RHYTHM_PATTERN("Rythme / Pattern"),
        SUMMARY_HUD("Résumé / HUD");

        private final String title;

        ConfigPage(String title) {
            this.title = title;
        }
    }

    private static final class ConfigEntry {
        private final String label;
        private final Supplier<String> value;
        private final Runnable forward;
        private final Runnable backward;
        private final String hint;

        private ConfigEntry(String label, Supplier<String> value, Runnable forward, Runnable backward, String hint) {
            this.label = label;
            this.value = value;
            this.forward = forward;
            this.backward = backward;
            this.hint = hint;
        }
    }
}
