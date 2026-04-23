package net.elysiastudios.client.gui;

import net.elysiastudios.client.config.ConfigManager;
import net.elysiastudios.client.hud.VanillaHudElement;
import net.elysiastudios.client.hud.VanillaHudRegistry;
import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.HudWidgetProvider;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private static final int VANILLA_COLOR = 0xFF38BDF8;
    private static final int MODULE_COLOR = 0xFFA855F7;
    private static final int HIT_PADDING = 4;

    private Screen parent;
    private HudWidget draggingWidget;
    private HudWidget selectedWidget;
    private VanillaHudElement draggingVanilla;
    private VanillaHudElement selectedVanilla;
    private double dragOffsetX;
    private double dragOffsetY;

    public HudEditorScreen() {
        super(Component.literal("Éditeur HUD"));
    }

    public HudEditorScreen(Screen parent) {
        this();
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xC0050810);
        drawGrid(guiGraphics);
        drawHeader(guiGraphics);

        VanillaHudElement hoveredVanilla = findVanillaAt(mouseX, mouseY);
        HudWidget hoveredWidget = findWidgetAt(mouseX, mouseY);

        for (VanillaHudElement element : VanillaHudRegistry.getElements()) {
            Bounds bounds = getVanillaBounds(element);
            boolean active = element == hoveredVanilla || element == selectedVanilla || element == draggingVanilla;
            drawVanillaPreview(guiGraphics, element, bounds, active);
            drawElementFrame(guiGraphics, bounds, element.getDisplayName(), VANILLA_COLOR, active, true);
        }

        ModuleManager moduleManager = ModuleManager.getInstance();
        if (moduleManager != null) {
            for (Module module : moduleManager.getModules()) {
                if (module instanceof HudWidgetProvider provider) {
                    for (HudWidget widget : provider.getHudWidgets()) {
                        if (!shouldRenderWidgetInEditor(module, widget)) {
                            continue;
                        }

                        Bounds bounds = getWidgetBounds(widget);
                        drawModuleSurface(guiGraphics, bounds);
                        widget.render(guiGraphics, true);

                        boolean active = widget == hoveredWidget || widget == selectedWidget || widget == draggingWidget;
                        drawElementFrame(guiGraphics, bounds, widget.getDisplayName(), MODULE_COLOR, active, false);
                    }
                }
            }
        }

        drawInspector(guiGraphics, hoveredVanilla, hoveredWidget);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 1) {
            if (resetHoveredElement(mouseX, mouseY)) {
                return true;
            }
        }

        VanillaHudElement vanilla = findVanillaAt(mouseX, mouseY);
        if (vanilla != null) {
            Bounds bounds = getVanillaBounds(vanilla);
            draggingVanilla = vanilla;
            draggingWidget = null;
            selectedVanilla = vanilla;
            selectedWidget = null;
            dragOffsetX = mouseX - bounds.x;
            dragOffsetY = mouseY - bounds.y;
            return true;
        }

        HudWidget widget = findWidgetAt(mouseX, mouseY);
        if (widget != null) {
            Bounds bounds = getWidgetBounds(widget);
            draggingWidget = widget.allowMoveInHudEditor() ? widget : null;
            draggingVanilla = null;
            selectedWidget = widget;
            selectedVanilla = null;
            dragOffsetX = mouseX - bounds.x;
            dragOffsetY = mouseY - bounds.y;
            return true;
        }

        selectedWidget = null;
        selectedVanilla = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (draggingWidget != null) {
            Bounds bounds = getWidgetBounds(draggingWidget);
            ConfigManager.HudConfig config = draggingWidget.getConfig();
            config.x = clamp((int) (mouseX - dragOffsetX), 0, Math.max(0, width - bounds.w));
            config.y = clamp((int) (mouseY - dragOffsetY), 0, Math.max(0, height - bounds.h));
            return true;
        }

        if (draggingVanilla != null) {
            Bounds bounds = getVanillaBounds(draggingVanilla);
            ConfigManager.HudConfig config = draggingVanilla.getConfig();
            int newX = clamp((int) (mouseX - dragOffsetX), 0, Math.max(0, width - bounds.w));
            int newY = clamp((int) (mouseY - dragOffsetY), 0, Math.max(0, height - bounds.h));
            config.x = newX - draggingVanilla.getBaseX(width, height);
            config.y = newY - draggingVanilla.getBaseY(width, height);
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWidget = null;
        draggingVanilla = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float delta = verticalAmount > 0 ? 0.1F : -0.1F;

        VanillaHudElement vanilla = findVanillaAt(mouseX, mouseY);
        if (vanilla != null) {
            ConfigManager.HudConfig config = vanilla.getConfig();
            config.scale = clamp(config.scale + delta, 0.5F, 2.5F);
            selectedVanilla = vanilla;
            selectedWidget = null;
            return true;
        }

        HudWidget widget = findWidgetAt(mouseX, mouseY);
        if (widget != null && widget.allowMoveInHudEditor()) {
            ConfigManager.HudConfig config = widget.getConfig();
            config.scale = clamp(config.scale + delta, 0.5F, 2.5F);
            selectedWidget = widget;
            selectedVanilla = null;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        ConfigManager.getInstance().save();
        if (minecraft != null && parent != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        int panelWidth = Math.min(width - 24, 760);
        int panelX = (width - panelWidth) / 2;
        int panelY = 8;
        fillPanel(guiGraphics, panelX, panelY, panelWidth, 48, 0xAA07111D, 0x6638BDF8);
        guiGraphics.drawCenteredString(font, trimToWidth("Éditeur HUD", panelWidth - 24), width / 2, panelY + 8, 0xFF38BDF8);
        guiGraphics.drawCenteredString(font, trimToWidth("Glisser = déplacer | Molette = échelle | Clic droit = réinitialiser | Bleu = Minecraft, violet = Elysia", panelWidth - 24), width / 2, panelY + 27, 0xFFD7E3F4);
    }

    private void drawInspector(GuiGraphics guiGraphics, VanillaHudElement hoveredVanilla, HudWidget hoveredWidget) {
        String label = "Survole un cadre pour voir l'élément exact";
        int color = 0xFF94A3B8;

        if (draggingVanilla != null) {
            label = "Déplacement Minecraft : " + draggingVanilla.getDisplayName();
            color = VANILLA_COLOR;
        } else if (draggingWidget != null) {
            label = "Déplacement Elysia : " + draggingWidget.getDisplayName();
            color = MODULE_COLOR;
        } else if (hoveredVanilla != null) {
            label = "HUD Minecraft : " + hoveredVanilla.getDisplayName();
            color = VANILLA_COLOR;
        } else if (hoveredWidget != null) {
            label = "Widget Elysia : " + hoveredWidget.getDisplayName();
            color = MODULE_COLOR;
        } else if (selectedVanilla != null) {
            label = "Sélection : " + selectedVanilla.getDisplayName();
            color = VANILLA_COLOR;
        } else if (selectedWidget != null) {
            label = "Sélection : " + selectedWidget.getDisplayName();
            color = MODULE_COLOR;
        }

        String visibleLabel = trimToWidth(label, Math.max(40, width - 48));
        int textWidth = font.width(visibleLabel);
        int x = 12;
        int y = height - 30;
        fillPanel(guiGraphics, x, y, textWidth + 24, 20, 0xB007111D, color & 0x55FFFFFF);
        guiGraphics.drawString(font, visibleLabel, x + 12, y + 6, color, true);
    }

    private void drawVanillaPreview(GuiGraphics guiGraphics, VanillaHudElement element, Bounds bounds, boolean active) {
        int fill = active ? 0x3328BDF8 : 0x181E293B;
        guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.w, bounds.y + bounds.h, fill);

        switch (element.getId()) {
            case "scoreboard" -> drawScoreboardPreview(guiGraphics, bounds);
            case "bossbar" -> drawBossBarPreview(guiGraphics, bounds);
            case "effects" -> drawEffectsPreview(guiGraphics, bounds);
            case "hotbar" -> drawHotbarPreview(guiGraphics, bounds);
            default -> guiGraphics.drawCenteredString(font, element.getDisplayName(), bounds.x + bounds.w / 2, bounds.y + bounds.h / 2 - 4, 0xCCFFFFFF);
        }
    }

    private void drawScoreboardPreview(GuiGraphics guiGraphics, Bounds bounds) {
        guiGraphics.fill(bounds.x + 4, bounds.y + 4, bounds.x + bounds.w - 4, bounds.y + 18, 0xAA0F172A);
        guiGraphics.drawString(font, "Server Board", bounds.x + 10, bounds.y + 8, 0xFFFFFFFF, true);
        int lines = Math.max(4, Math.min(8, (bounds.h - 26) / 14));
        for (int i = 0; i < lines; i++) {
            int y = bounds.y + 24 + i * 14;
            int lineW = Math.max(28, bounds.w - 24 - i * 6);
            guiGraphics.fill(bounds.x + 10, y, bounds.x + 10 + lineW, y + 7, 0x5538BDF8);
        }
    }

    private void drawBossBarPreview(GuiGraphics guiGraphics, Bounds bounds) {
        int barY = bounds.y + Math.max(5, bounds.h / 2 - 4);
        guiGraphics.fill(bounds.x + 8, barY, bounds.x + bounds.w - 8, barY + 8, 0xAA111827);
        guiGraphics.fill(bounds.x + 8, barY, bounds.x + Math.max(8, bounds.w * 2 / 3), barY + 8, 0xFFB91C1C);
    }

    private void drawEffectsPreview(GuiGraphics guiGraphics, Bounds bounds) {
        int count = Math.max(2, Math.min(4, bounds.h / 18));
        for (int i = 0; i < count; i++) {
            int y = bounds.y + 8 + i * 18;
            guiGraphics.fill(bounds.x + 8, y, bounds.x + 22, y + 14, 0xAA22C55E);
            guiGraphics.fill(bounds.x + 28, y + 3, bounds.x + bounds.w - 8, y + 10, 0x5538BDF8);
        }
    }

    private void drawHotbarPreview(GuiGraphics guiGraphics, Bounds bounds) {
        int slotCount = 9;
        int gap = 2;
        int slot = Math.max(8, Math.min(20, (bounds.w - gap * (slotCount - 1)) / slotCount));
        int total = slot * slotCount + gap * (slotCount - 1);
        int x = bounds.x + (bounds.w - total) / 2;
        int y = bounds.y + Math.max(2, (bounds.h - slot) / 2);
        for (int i = 0; i < slotCount; i++) {
            int sx = x + i * (slot + gap);
            guiGraphics.fill(sx, y, sx + slot, y + slot, i == 0 ? 0x8838BDF8 : 0xAA111827);
            guiGraphics.renderOutline(sx, y, slot, slot, i == 0 ? 0xFF38BDF8 : 0x6638BDF8);
        }
    }

    private void drawModuleSurface(GuiGraphics guiGraphics, Bounds bounds) {
        boolean compact = bounds.h <= 8;
        int alpha = compact ? 0x30 : 0x18;
        guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.w, bounds.y + bounds.h, (alpha << 24) | 0x12081D);
        if (compact) {
            guiGraphics.fill(bounds.x, bounds.y, bounds.x + bounds.w, bounds.y + Math.max(1, bounds.h), 0x55A855F7);
        }
    }

    private void drawElementFrame(GuiGraphics guiGraphics, Bounds bounds, String label, int color, boolean active, boolean vanilla) {
        int frameColor = (active ? 0xFF : 0xAA) << 24 | (color & 0x00FFFFFF);
        int glowColor = (active ? 0x36 : 0x1A) << 24 | (color & 0x00FFFFFF);

        guiGraphics.fill(bounds.x - 3, bounds.y - 3, bounds.x + bounds.w + 3, bounds.y + bounds.h + 3, glowColor);
        guiGraphics.renderOutline(bounds.x - 2, bounds.y - 2, bounds.w + 4, bounds.h + 4, frameColor);
        guiGraphics.renderOutline(bounds.x - 4, bounds.y - 4, bounds.w + 8, bounds.h + 8, active ? frameColor : 0x33000000 | (color & 0x00FFFFFF));

        if (active) {
            String tag = (vanilla ? "MC  " : "MOD ") + label;
            tag = trimToWidth(tag, Math.max(24, width - 16));
            int chipW = font.width(tag) + 12;
            int chipX = clamp(bounds.x - 2, 4, Math.max(4, width - chipW - 4));
            int chipY = bounds.y - 16;
            if (chipY < 4) {
                chipY = bounds.y + 4;
            }
            guiGraphics.fill(chipX, chipY, chipX + chipW, chipY + 13, 0xE007111D);
            guiGraphics.fill(chipX, chipY + 12, chipX + chipW, chipY + 13, frameColor);
            guiGraphics.drawString(font, tag, chipX + 6, chipY + 3, 0xFFFFFFFF, true);
        }
        if (active) {
            guiGraphics.fill(bounds.x + bounds.w - 5, bounds.y + bounds.h - 5, bounds.x + bounds.w + 2, bounds.y + bounds.h + 2, frameColor);
        }
    }

    private VanillaHudElement findVanillaAt(double mouseX, double mouseY) {
        for (int i = VanillaHudRegistry.getElements().size() - 1; i >= 0; i--) {
            VanillaHudElement element = VanillaHudRegistry.getElements().get(i);
            Bounds bounds = getVanillaBounds(element);
            if (isInElement(mouseX, mouseY, bounds, element.getDisplayName())) {
                return element;
            }
        }
        return null;
    }

    private HudWidget findWidgetAt(double mouseX, double mouseY) {
        ModuleManager moduleManager = ModuleManager.getInstance();
        if (moduleManager == null) {
            return null;
        }

        HudWidget found = null;
        for (Module module : moduleManager.getModules()) {
            if (module instanceof HudWidgetProvider provider) {
                for (HudWidget widget : provider.getHudWidgets()) {
                    if (!shouldRenderWidgetInEditor(module, widget)) {
                        continue;
                    }

                    Bounds bounds = getWidgetBounds(widget);
                    if (isInElement(mouseX, mouseY, bounds, widget.getDisplayName())) {
                        found = widget;
                    }
                }
            }
        }
        return found;
    }

    private boolean resetHoveredElement(double mouseX, double mouseY) {
        VanillaHudElement vanilla = findVanillaAt(mouseX, mouseY);
        if (vanilla != null) {
            ConfigManager.HudConfig config = vanilla.getConfig();
            config.x = 0;
            config.y = 0;
            config.scale = 1.0F;
            selectedVanilla = vanilla;
            selectedWidget = null;
            return true;
        }

        HudWidget widget = findWidgetAt(mouseX, mouseY);
        if (widget != null) {
            ConfigManager.HudConfig config = widget.getConfig();
            config.x = widget.getDefaultX();
            config.y = widget.getDefaultY();
            config.scale = widget.getDefaultScale();
            config.visible = widget.isDefaultVisible();
            selectedWidget = widget;
            selectedVanilla = null;
            return true;
        }

        return false;
    }

    private Bounds getWidgetBounds(HudWidget widget) {
        ConfigManager.HudConfig config = widget.getConfig();
        int w = Math.max(8, Math.round(widget.getEditorWidth() * config.scale));
        int h = Math.max(6, Math.round(widget.getEditorHeight() * config.scale));
        return new Bounds(config.x, config.y, w, h);
    }

    private Bounds getVanillaBounds(VanillaHudElement element) {
        ConfigManager.HudConfig config = element.getConfig();
        int x = element.getBaseX(width, height) + config.x;
        int y = element.getBaseY(width, height) + config.y;
        int w = Math.max(32, Math.round(element.getDefaultWidth() * config.scale));
        int h = Math.max(18, Math.round(element.getDefaultHeight() * config.scale));
        return new Bounds(x, y, w, h);
    }

    private boolean isInElement(double mouseX, double mouseY, Bounds bounds, String label) {
        return isInside(mouseX, mouseY, bounds.x - HIT_PADDING, bounds.y - HIT_PADDING, bounds.w + HIT_PADDING * 2, bounds.h + HIT_PADDING * 2);
    }

    private void drawGrid(GuiGraphics guiGraphics) {
        for (int x = 0; x < width; x += 20) {
            guiGraphics.fill(x, 0, x + 1, height, 0x141E293B);
        }
        for (int y = 0; y < height; y += 20) {
            guiGraphics.fill(0, y, width, y + 1, 0x141E293B);
        }
        for (int x = 0; x < width; x += 100) {
            guiGraphics.fill(x, 0, x + 1, height, 0x22233446);
        }
        for (int y = 0; y < height; y += 100) {
            guiGraphics.fill(0, y, width, y + 1, 0x22233446);
        }
    }

    private void fillPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int fill, int outline) {
        guiGraphics.fill(x, y, x + w, y + h, fill);
        guiGraphics.renderOutline(x, y, w, h, outline);
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

    private boolean shouldRenderWidgetInEditor(Module module, HudWidget widget) {
        return widget != null && module.isEnabled() && widget.isVisible() && widget.shouldRenderInHudEditor();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Bounds {
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private Bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
