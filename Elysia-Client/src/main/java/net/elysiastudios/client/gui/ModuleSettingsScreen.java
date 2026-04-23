package net.elysiastudios.client.gui;

import net.elysiastudios.client.config.ConfigManager;
import net.elysiastudios.client.module.HudWidgetProvider;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.setting.KeybindModuleSetting;
import net.elysiastudios.client.setting.ModuleSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;
    private final List<String> sections;
    private int selectedSection;
    private double scrollAmount;
    private KeybindModuleSetting awaitingKeybind;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getName()));
        this.parent = parent;
        this.module = module;
        this.sections = collectSections(module);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xC0080C12);

        int panelWidth = Math.min(width - 40, 760);
        int panelHeight = Math.min(height - 40, 470);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        fillRoundedRect(guiGraphics, panelX - 3, panelY - 3, panelWidth + 6, panelHeight + 6, 18, 0x5538BDF8);
        fillRoundedRect(guiGraphics, panelX, panelY, panelWidth, panelHeight, 16, 0xF010131B);

        int buttonY = panelY + 14;
        int buttonWidth = 96;
        int gap = 10;
        int rightEdge = panelX + panelWidth - 20;
        int resetX = rightEdge - buttonWidth;
        int hudX = resetX - gap - buttonWidth;

        boolean hasHudButton = module instanceof HudWidgetProvider || module.isHud();
        int actionsWidth = buttonWidth + (hasHudButton ? buttonWidth + gap : 0);
        int headerTextWidth = Math.max(110, panelWidth - 40 - actionsWidth - 12);
        String subtitle = module.getCategory().getName() + " • " + module.getDescription();
        guiGraphics.drawString(font, trimToWidth(module.getName(), headerTextWidth), panelX + 20, panelY + 16, 0xFFFFFFFF, true);
        guiGraphics.drawString(font, trimToWidth(subtitle, headerTextWidth), panelX + 20, panelY + 31, 0xFF94A3B8, false);

        if (hasHudButton) {
            boolean hudHovered = isInside(mouseX, mouseY, hudX, buttonY, buttonWidth, 22);
            fillRoundedRect(guiGraphics, hudX, buttonY, buttonWidth, 22, 8, hudHovered ? 0xFF2563EB : 0xCC1E40AF);
            guiGraphics.drawCenteredString(font, "Éditeur HUD", hudX + buttonWidth / 2, buttonY + 7, 0xFFFFFFFF);
        }

        boolean resetHovered = isInside(mouseX, mouseY, resetX, buttonY, buttonWidth, 22);
        fillRoundedRect(guiGraphics, resetX, buttonY, buttonWidth, 22, 8, resetHovered ? 0xFFB91C1C : 0xCC7F1D1D);
        guiGraphics.drawCenteredString(font, "Réinitialiser", resetX + buttonWidth / 2, buttonY + 7, 0xFFFFFFFF);

        int tabsY = panelY + 54;
        int tabX = panelX + 20;
        int tabGap = 8;
        int maxTabWidth = Math.max(52, (panelWidth - 40 - (Math.max(0, sections.size() - 1) * tabGap)) / Math.max(1, sections.size()));
        for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i);
            boolean selected = i == selectedSection;
            int tabWidth = Math.min(font.width(section) + 22, maxTabWidth);
            fillRoundedRect(guiGraphics, tabX, tabsY, tabWidth, 20, 8, selected ? 0xFF1D4ED8 : 0x331E293B);
            guiGraphics.drawString(font, trimToWidth(section, tabWidth - 14), tabX + 8, tabsY + 6, selected ? 0xFFFFFFFF : 0xFFCBD5E1, false);
            tabX += tabWidth + tabGap;
        }

        List<ModuleSetting<?>> settings = getCurrentSectionSettings();
        int contentX = panelX + 20;
        int contentY = panelY + 88;
        int contentWidth = panelWidth - 40;
        int contentHeight = panelHeight - 128;
        int rowHeight = 32;
        int gapY = 8;
        int totalHeight = settings.size() * (rowHeight + gapY);
        int maxScroll = Math.max(0, totalHeight - contentHeight);
        scrollAmount = Math.max(0.0D, Math.min(scrollAmount, maxScroll));

        ModuleSetting<?> hoveredSetting = null;
        guiGraphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        int drawY = contentY - (int) scrollAmount;
        for (ModuleSetting<?> setting : settings) {
            boolean hovered = isInside(mouseX, mouseY, contentX, drawY, contentWidth, rowHeight);
            if (hovered) {
                hoveredSetting = setting;
            }

            int background = hovered ? 0xFF151D2A : 0xCC0D131C;
            if (awaitingKeybind == setting) {
                background = 0xFF1D4ED8;
            }
            fillRoundedRect(guiGraphics, contentX, drawY, contentWidth, rowHeight, 10, background);

            String value = awaitingKeybind == setting ? "Appuie sur une touche..." : setting.getDisplayValue();
            int valueMaxWidth = Math.max(70, Math.min(180, contentWidth / 2));
            String visibleValue = trimToWidth(value, valueMaxWidth);
            int valueX = contentX + contentWidth - 12 - font.width(visibleValue);
            String visibleLabel = trimToWidth(setting.getLabel(), Math.max(40, valueX - contentX - 24));
            guiGraphics.drawString(font, visibleLabel, contentX + 12, drawY + 12, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, visibleValue, valueX, drawY + 12, hovered ? 0xFF60A5FA : 0xFF94A3B8, false);

            drawY += rowHeight + gapY;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 10;
            int scrollbarY = contentY;
            int scrollbarHeight = contentHeight;
            int thumbHeight = Math.max(28, (int) ((contentHeight / (double) totalHeight) * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((scrollAmount / maxScroll) * (scrollbarHeight - thumbHeight));
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x221E293B);
            fillRoundedRect(guiGraphics, scrollbarX, thumbY, 4, thumbHeight, 2, 0xFF3B82F6);
        }

        String hint;
        if (awaitingKeybind != null) {
            hint = "Appuie sur une touche pour enregistrer le bind. Échap supprime le bind.";
        } else if (hoveredSetting != null) {
            hint = hoveredSetting.getDescription();
        } else {
            hint = "Clic gauche = suivant / augmenter. Clic droit = précédent / diminuer.";
        }

        guiGraphics.drawString(font, trimToWidth(hint, panelWidth - 40), panelX + 20, panelY + panelHeight - 28, 0xFF94A3B8, false);
        guiGraphics.drawString(font, trimToWidth("Chaque module possède maintenant sa page de réglages.", panelWidth - 40), panelX + 20, panelY + panelHeight - 16, 0xFF64748B, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int panelWidth = Math.min(width - 40, 760);
        int panelHeight = Math.min(height - 40, 470);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        int buttonY = panelY + 14;
        int buttonWidth = 96;
        int gap = 10;
        int rightEdge = panelX + panelWidth - 20;
        int resetX = rightEdge - buttonWidth;
        int hudX = resetX - gap - buttonWidth;

        if ((module instanceof HudWidgetProvider || module.isHud()) && isInside(mouseX, mouseY, hudX, buttonY, buttonWidth, 22)) {
            ConfigManager.getInstance().save();
            minecraft.setScreen(new HudEditorScreen(this));
            return true;
        }

        if (isInside(mouseX, mouseY, resetX, buttonY, buttonWidth, 22)) {
            module.resetSettings();
            awaitingKeybind = null;
            ConfigManager.getInstance().save();
            return true;
        }

        int tabsY = panelY + 54;
        int tabX = panelX + 20;
        int tabGap = 8;
        int maxTabWidth = Math.max(52, (panelWidth - 40 - (Math.max(0, sections.size() - 1) * tabGap)) / Math.max(1, sections.size()));
        for (int i = 0; i < sections.size(); i++) {
            int tabWidth = Math.min(font.width(sections.get(i)) + 22, maxTabWidth);
            if (isInside(mouseX, mouseY, tabX, tabsY, tabWidth, 20)) {
                selectedSection = i;
                scrollAmount = 0.0D;
                awaitingKeybind = null;
                return true;
            }
            tabX += tabWidth + tabGap;
        }

        List<ModuleSetting<?>> settings = getCurrentSectionSettings();
        int contentX = panelX + 20;
        int contentY = panelY + 88;
        int contentWidth = panelWidth - 40;
        int rowHeight = 32;
        int gapY = 8;
        int drawY = contentY - (int) scrollAmount;
        for (ModuleSetting<?> setting : settings) {
            if (isInside(mouseX, mouseY, contentX, drawY, contentWidth, rowHeight)) {
                if (setting instanceof KeybindModuleSetting keybindSetting) {
                    if (button == 1) {
                        keybindSetting.clearBinding();
                        awaitingKeybind = null;
                        ConfigManager.getInstance().save();
                    } else {
                        awaitingKeybind = keybindSetting;
                    }
                    return true;
                }

                awaitingKeybind = null;
                if (button == 1) {
                    setting.stepBackward();
                } else {
                    setting.stepForward();
                }
                ConfigManager.getInstance().save();
                return true;
            }
            drawY += rowHeight + gapY;
        }

        awaitingKeybind = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAmount = Math.max(0.0D, scrollAmount - verticalAmount * 24.0D);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (awaitingKeybind != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                awaitingKeybind.clearBinding();
            } else {
                awaitingKeybind.setValue(event.key());
            }
            awaitingKeybind = null;
            ConfigManager.getInstance().save();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ConfigManager.getInstance().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<ModuleSetting<?>> getCurrentSectionSettings() {
        String section = sections.get(Math.max(0, Math.min(selectedSection, sections.size() - 1)));
        List<ModuleSetting<?>> result = new ArrayList<>();
        for (ModuleSetting<?> setting : module.getSettings()) {
            if (section.equals(setting.getSection())) {
                result.add(setting);
            }
        }
        return result;
    }

    private static List<String> collectSections(Module module) {
        Set<String> ordered = new LinkedHashSet<>();
        for (ModuleSetting<?> setting : module.getSettings()) {
            ordered.add(setting.getSection());
        }
        if (ordered.isEmpty()) {
            ordered.add("Général");
        }
        return List.copyOf(ordered);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - font.width(ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(trimmed) > available) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    private void fillRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }

        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        for (int dy = 0; dy < h; dy++) {
            int inset = 0;
            if (dy < r) {
                double delta = r - dy - 0.5D;
                inset = Math.max(0, r - (int) Math.floor(Math.sqrt(Math.max(0.0D, (r * r) - (delta * delta)))));
            } else if (dy >= h - r) {
                double delta = dy - (h - r) + 0.5D;
                inset = Math.max(0, r - (int) Math.floor(Math.sqrt(Math.max(0.0D, (r * r) - (delta * delta)))));
            }
            guiGraphics.fill(x + inset, y + dy, x + w - inset, y + dy + 1, color);
        }
    }
}
