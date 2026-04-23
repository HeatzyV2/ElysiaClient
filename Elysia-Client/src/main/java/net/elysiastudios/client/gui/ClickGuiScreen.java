package net.elysiastudios.client.gui;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.ConfigurableModule;
import net.elysiastudios.client.module.Module;
import net.elysiastudios.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final long INTRO_DURATION_MS = 320L;
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int MAX_SIDEBAR_WIDTH = 150;
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("elysia-client", "logo");

    private final Screen parent;
    private Category selectedCategory = Category.HUD;
    private double scrollAmount;
    private long introAnimationStart = -1L;

    public ClickGuiScreen() {
        this(null);
    }

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Elysia Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        introAnimationStart = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Do not call renderBackground here: Fabric Screen API may already apply blur this frame.
        long time = System.currentTimeMillis();
        float introProgress = getIntroProgress(time);
        int panelOffset = introSlide(introProgress, height < 420 ? 14 : 22);
        int sidebarOffset = introSlide(delayedProgress(introProgress, 0.08F), 8);
        int contentOffset = introSlide(delayedProgress(introProgress, 0.16F), 12);
        int backdropAlpha = Math.max(0, Math.min(255, 160 + Math.round((1.0F - introProgress) * 28.0F)));

        guiGraphics.fill(0, 0, width, height, (backdropAlpha << 24) | 0x060810);

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int sidebarWidth = getSidebarWidth(panelWidth);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2 + panelOffset;

        fillRoundedRect(guiGraphics, panelX - 3, panelY - 3, panelWidth + 6, panelHeight + 6, 18, 0x5538BDF8);
        fillRoundedRect(guiGraphics, panelX, panelY, panelWidth, panelHeight, 16, 0xF010131B);
        fillRoundedRect(guiGraphics, panelX, panelY, sidebarWidth, panelHeight, 16, 0xFF141A24);

        int logoSize = getLogoSize(sidebarWidth, panelHeight);
        int logoX = panelX + (sidebarWidth - logoSize) / 2;
        int logoY = panelY + 14 + (sidebarOffset / 2);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, logoSize, logoSize);

        CategoryLayout categoryLayout = getCategoryLayout(panelHeight, logoSize);
        int categoryY = panelY + categoryLayout.startOffset + sidebarOffset;
        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hovered = isInside(mouseX, mouseY, panelX + 12, categoryY, sidebarWidth - 24, categoryLayout.itemHeight);
            int bg = selected ? 0xFF1D4ED8 : hovered ? 0x55233446 : 0x00000000;

            fillRoundedRect(guiGraphics, panelX + 12, categoryY, sidebarWidth - 24, categoryLayout.itemHeight, 8, bg);
            UiFontHelper.draw(guiGraphics, font, category.getName(), panelX + 24, categoryY + Math.max(6, (categoryLayout.itemHeight - 8) / 2), selected ? 0xFFFFFFFF : 0xFFCBD5E1, false);
            categoryY += categoryLayout.step;
        }

        int contentX = panelX + sidebarWidth + 22;
        int contentY = panelY + 22 + contentOffset;
        int contentWidth = panelWidth - sidebarWidth - 34;
        int contentHeight = panelHeight - 44;
        int hudButtonX = panelX + panelWidth - 138;
        int hudButtonY = panelY + 18 + contentOffset;
        boolean showHudButton = selectedCategory == Category.HUD;
        int titleMaxWidth = showHudButton ? Math.max(90, hudButtonX - contentX - 8) : contentWidth;

        UiFontHelper.draw(guiGraphics, font, trimToWidth(selectedCategory.getName(), titleMaxWidth), contentX, contentY, 0xFFFFFFFF, true);
        UiFontHelper.draw(guiGraphics, font, trimToWidth("Modules stables et orientés QoL", titleMaxWidth), contentX, contentY + 14, 0xFF94A3B8, false);
        UiFontHelper.draw(guiGraphics, font, trimToWidth("Clic droit sur un module pour ouvrir ses paramètres", contentWidth), contentX, contentY + 28, 0xFF64748B, false);

        if (showHudButton) {
            boolean hudButtonHovered = isInside(mouseX, mouseY, hudButtonX, hudButtonY, 116, 22);
            fillRoundedRect(guiGraphics, hudButtonX, hudButtonY, 116, 22, 8, hudButtonHovered ? 0xFF2563EB : 0xFF1E40AF);
            UiFontHelper.drawCentered(guiGraphics, font, "Éditer le HUD", hudButtonX + 58, hudButtonY + 7, 0xFFFFFFFF, false);
        }

        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
        int cardsY = contentY + 54;
        int viewHeight = Math.max(40, contentHeight - 66);
        int cardHeight = 48;
        int gap = 10;
        int totalHeight = modules.size() * (cardHeight + gap);
        int maxScroll = Math.max(0, totalHeight - viewHeight);
        scrollAmount = Math.max(0.0, Math.min(scrollAmount, maxScroll));

        guiGraphics.enableScissor(contentX, cardsY, contentX + contentWidth, cardsY + viewHeight);
        int drawY = cardsY - (int) scrollAmount;
        for (Module module : modules) {
            drawModuleCard(guiGraphics, module, contentX, drawY, contentWidth, mouseX, mouseY);
            drawY += cardHeight + gap;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 10;
            int scrollbarY = cardsY;
            int scrollbarHeight = viewHeight;
            int thumbHeight = Math.max(24, (int) ((viewHeight / (double) totalHeight) * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((scrollAmount / maxScroll) * (scrollbarHeight - thumbHeight));
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x221E293B);
            fillRoundedRect(guiGraphics, scrollbarX, thumbY, 4, thumbHeight, 2, 0xFF3B82F6);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawModuleCard(GuiGraphics guiGraphics, Module module, int x, int y, int cardWidth, int mouseX, int mouseY) {
        if (y + 48 < 0 || y > height) {
            return;
        }

        boolean hovered = isInside(mouseX, mouseY, x, y, cardWidth, 48);
        int background = hovered ? 0xFF151D2A : 0xDD0D131C;
        fillRoundedRect(guiGraphics, x, y, cardWidth, 48, 12, background);
        guiGraphics.fill(x, y, x + 4, y + 48, module.isEnabled() ? 0xFF22C55E : 0xFF334155);

        String badge = module.getName().substring(0, 1).toUpperCase();
        fillRoundedRect(guiGraphics, x + 12, y + 10, 22, 22, 8, 0xFF1E293B);
        UiFontHelper.drawCentered(guiGraphics, font, badge, x + 23, y + 17, 0xFFE2E8F0, false);

        int toggleX = x + cardWidth - 52;
        int toggleY = y + 16;
        int reservedWidth = module instanceof ConfigurableModule ? 102 : 58;
        int textWidth = Math.max(24, cardWidth - 44 - reservedWidth);
        UiFontHelper.draw(guiGraphics, font, trimToWidth(module.getName(), textWidth), x + 44, y + 11, 0xFFFFFFFF, true);
        UiFontHelper.draw(guiGraphics, font, trimToWidth(module.getDescription(), textWidth), x + 44, y + 25, 0xFF94A3B8, false);

        if (module instanceof ConfigurableModule) {
            fillRoundedRect(guiGraphics, toggleX - 42, toggleY, 34, 16, 8, hovered ? 0xFF1D4ED8 : 0x552563EB);
            UiFontHelper.drawCentered(guiGraphics, font, "CFG", toggleX - 25, toggleY + 4, 0xFFFFFFFF, false);
        }
        fillRoundedRect(guiGraphics, toggleX, toggleY, 32, 16, 8, module.isEnabled() ? 0xFF16A34A : 0xFF475569);
        int knobX = module.isEnabled() ? toggleX + 17 : toggleX + 2;
        fillRoundedRect(guiGraphics, knobX, toggleY + 2, 13, 12, 6, 0xFFF8FAFC);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        float introProgress = getIntroProgress(System.currentTimeMillis());
        int panelOffset = introSlide(introProgress, height < 420 ? 14 : 22);
        int sidebarOffset = introSlide(delayedProgress(introProgress, 0.08F), 8);
        int contentOffset = introSlide(delayedProgress(introProgress, 0.16F), 12);
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int sidebarWidth = getSidebarWidth(panelWidth);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2 + panelOffset;

        int logoSize = getLogoSize(sidebarWidth, panelHeight);
        CategoryLayout categoryLayout = getCategoryLayout(panelHeight, logoSize);
        int categoryY = panelY + categoryLayout.startOffset + sidebarOffset;
        for (Category category : Category.values()) {
            if (isInside(mouseX, mouseY, panelX + 12, categoryY, sidebarWidth - 24, categoryLayout.itemHeight)) {
                selectedCategory = category;
                scrollAmount = 0.0;
                return true;
            }
            categoryY += categoryLayout.step;
        }

        if (selectedCategory == Category.HUD && isInside(mouseX, mouseY, panelX + panelWidth - 138, panelY + 18 + contentOffset, 116, 22)) {
            minecraft.setScreen(new HudEditorScreen(this));
            return true;
        }

        int contentX = panelX + sidebarWidth + 22;
        int contentY = panelY + 76 + contentOffset;
        int contentWidth = panelWidth - sidebarWidth - 34;
        int drawY = contentY - (int) scrollAmount;
        for (Module module : ModuleManager.getInstance().getModulesByCategory(selectedCategory)) {
            if (isInside(mouseX, mouseY, contentX, drawY, contentWidth, 48)) {
                if (button == 1 && module instanceof ConfigurableModule configurableModule) {
                    minecraft.setScreen(configurableModule.createConfigScreen(this));
                    return true;
                }

                if (button == 0) {
                    module.toggle();
                    return true;
                }
            }
            drawY += 58;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAmount = Math.max(0.0, scrollAmount - verticalAmount * 28.0);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    private int getPanelWidth() {
        return Math.max(260, Math.min(MAX_PANEL_WIDTH, width - 20));
    }

    private int getPanelHeight() {
        return Math.max(220, Math.min(MAX_PANEL_HEIGHT, height - 20));
    }

    private int getSidebarWidth(int panelWidth) {
        return Math.min(MAX_SIDEBAR_WIDTH, Math.max(105, panelWidth / 4));
    }

    private int getLogoSize(int sidebarWidth, int panelHeight) {
        return Math.min(58, Math.max(38, Math.min(sidebarWidth - 56, panelHeight / 5)));
    }

    private CategoryLayout getCategoryLayout(int panelHeight, int logoSize) {
        int count = Math.max(1, Category.values().length);
        int desiredHeight = panelHeight < 260 ? 22 : 26;
        int startOffset = Math.max(58, 28 + logoSize);
        int availableBetweenTops = panelHeight - 14 - startOffset - desiredHeight;
        int step = count <= 1 ? desiredHeight + 2 : availableBetweenTops / (count - 1);
        int itemHeight = Math.min(desiredHeight, Math.max(18, step - 2));
        step = Math.max(itemHeight + 2, Math.min(32, step));

        return new CategoryLayout(startOffset, itemHeight, step);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (UiFontHelper.width(font, text) <= maxWidth) {
            return text == null ? "" : text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - UiFontHelper.width(font, ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && UiFontHelper.width(font, trimmed) > available) {
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
                double delta = r - dy - 0.5;
                inset = Math.max(0, r - (int) Math.floor(Math.sqrt(Math.max(0.0, (r * r) - (delta * delta)))));
            } else if (dy >= h - r) {
                double delta = dy - (h - r) + 0.5;
                inset = Math.max(0, r - (int) Math.floor(Math.sqrt(Math.max(0.0, (r * r) - (delta * delta)))));
            }
            guiGraphics.fill(x + inset, y + dy, x + w - inset, y + dy + 1, color);
        }
    }

    private float getIntroProgress(long time) {
        if (introAnimationStart <= 0L) {
            return 1.0F;
        }
        float raw = Math.max(0.0F, Math.min(1.0F, (time - introAnimationStart) / (float) INTRO_DURATION_MS));
        return easeOutCubic(raw);
    }

    private float delayedProgress(float progress, float delay) {
        if (progress <= delay) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (progress - delay) / (1.0F - delay)));
    }

    private float easeOutCubic(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        float inverse = 1.0F - clamped;
        return 1.0F - (inverse * inverse * inverse);
    }

    private int introSlide(float progress, int distance) {
        return Math.round((1.0F - progress) * distance);
    }

    private static final class CategoryLayout {
        private final int startOffset;
        private final int itemHeight;
        private final int step;

        private CategoryLayout(int startOffset, int itemHeight, int step) {
            this.startOffset = startOffset;
            this.itemHeight = itemHeight;
            this.step = step;
        }
    }
}
