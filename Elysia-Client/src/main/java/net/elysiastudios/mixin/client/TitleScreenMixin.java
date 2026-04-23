package net.elysiastudios.mixin.client;

import net.elysiastudios.client.gui.UiFontHelper;
import net.elysiastudios.client.gui.ClickGuiScreen;
import net.elysiastudios.client.gui.HudEditorScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique private static final long MENU_INTRO_DURATION_MS = 420L;
    @Unique private static final Identifier LOGO = Identifier.fromNamespaceAndPath("elysia-client", "logo");
    @Unique private static final Identifier SOCIAL_DISCORD_ICON = Identifier.fromNamespaceAndPath("elysia-client", "social_discord");
    @Unique private static final Identifier SOCIAL_SITE_ICON = Identifier.fromNamespaceAndPath("elysia-client", "social_site");
    @Unique private static final Identifier[] SOCIAL_ICONS = {SOCIAL_DISCORD_ICON, SOCIAL_SITE_ICON};

    @Unique private static final String WEBSITE_URL = "https://client.elysiastudios.net";
    @Unique private static final String DISCORD_URL = "https://discord.gg/elysiastudios";

    @Unique private static final String[] PRIMARY_LABELS = {"SOLO", "MULTIJOUEUR", "MENU CLIENT", "MODS"};
    @Unique private static final String[] PRIMARY_HINTS = {
        "Mondes locaux, captures et profils",
        "Serveurs, favoris et connexions rapides",
        "Modules, ClickGUI et reglages Elysia",
        "Client Elysia, modules et organisation"
    };
    @Unique private static final int[] PRIMARY_ACCENTS = {0xFF8B5CF6, 0xFF38BDF8, 0xFFA855F7, 0xFFF59E0B};

    @Unique private static final String[] SOCIAL_LABELS = {"Discord", "Site"};
    @Unique private static final int[] SOCIAL_ACCENTS = {0xFF5865F2, 0xFF38BDF8};

    @Unique private static final String[] UTILITY_LABELS = {"OPTIONS", "QUITTER"};
    @Unique private static final int[] UTILITY_ACCENTS = {0xFF60A5FA, 0xFFEF4444};

    @Unique private static final String[] DOCK_LABELS = {"SHIFT MENU", "HUD EDITOR"};
    @Unique private static final String[] DOCK_HINTS = {"Ouvre le menu principal du client", "Editeur des widgets Elysia"};
    @Unique private static final String[] DOCK_BADGES = {"RSHIFT", "EDITOR"};
    @Unique private static final int[] DOCK_ACCENTS = {0xFFA855F7, 0xFF22D3EE};

    @Unique private final CubeMap elysia$cubeMap = new CubeMap(Identifier.withDefaultNamespace("textures/gui/title/background/panorama"));
    @Unique private final PanoramaRenderer elysia$panorama = new PanoramaRenderer(elysia$cubeMap);
    @Unique private long elysia$menuAnimationStart = -1L;
    @Unique private long elysia$lastFrameTime = -1L;
    @Unique private float elysia$logoHoverProgress = 0.0F;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.elysia$menuAnimationStart = System.currentTimeMillis();
        this.elysia$lastFrameTime = this.elysia$menuAnimationStart;
        this.elysia$logoHoverProgress = 0.0F;
        for (Renderable renderable : ((ScreenAccessor) this).getRenderables()) {
            if (renderable instanceof AbstractWidget widget) {
                widget.visible = false;
                widget.active = false;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderCustom(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int screenWidth = this.width;
        int screenHeight = this.height;
        long time = System.currentTimeMillis();
        float introProgress = getIntroProgress(time);
        int modsLoaded = FabricLoader.getInstance().getAllMods().size();
        MenuLayout layout = buildLayout(screenWidth, screenHeight);

        elysia$panorama.render(graphics, screenWidth, screenHeight, true);
        drawAtmosphere(graphics, layout, time);
        drawTopRows(graphics, layout, mouseX, mouseY, time, introProgress);
        drawHero(graphics, layout, mouseX, mouseY, time, introProgress);
        drawDock(graphics, layout, mouseX, mouseY, time, introProgress);
        drawFooter(graphics, layout, modsLoaded, introProgress);
        drawIntroOverlay(graphics, layout.screenWidth(), layout.screenHeight(), introProgress);

        ci.cancel();
    }

    @Unique
    private MenuLayout buildLayout(int screenWidth, int screenHeight) {
        boolean tiny = screenWidth < 680 || screenHeight < 470;
        boolean compact = screenWidth < 1100 || screenHeight < 640;
        boolean wide = screenWidth >= 1180 && screenHeight >= 640;

        int margin = tiny ? 12 : compact ? 18 : 24;
        int topRowY = margin;

        int topButtonHeight = tiny ? 28 : 32;
        boolean socialIconOnly = screenWidth < 820 || screenHeight < 520;
        int socialHeight = tiny ? 24 : 27;
        int socialButtonWidth = socialIconOnly ? socialHeight : (tiny ? 72 : 84);
        int socialGap = 6;
        int socialX = margin;

        boolean utilityCompact = screenWidth < 760 || screenHeight < 520;
        int utilityHeight = tiny ? 24 : 27;
        int utilityGap = 6;
        int utilityPadding = tiny ? 10 : 14;
        int utilityFirstWidth = clamp(UiFontHelper.width(this.font, UTILITY_LABELS[0]) + (utilityPadding * 2), tiny ? 58 : 64, utilityCompact ? 76 : 88);
        int utilitySecondWidth = clamp(UiFontHelper.width(this.font, UTILITY_LABELS[1]) + (utilityPadding * 2), tiny ? 58 : 64, utilityCompact ? 76 : 88);
        int utilityRowWidth = utilityFirstWidth + utilitySecondWidth + utilityGap;
        int utilityX = screenWidth - margin - utilityRowWidth;

        boolean stackedFooter = screenWidth < 620;

        int heroWidth = clamp(screenWidth - (margin * 2), tiny ? 260 : compact ? 310 : 344, compact ? 352 : 380);
        int heroPaddingX = 0;
        int heroPaddingTop = tiny ? 10 : 14;
        int heroPaddingBottom = tiny ? 8 : 12;
        int heroRadius = tiny ? 18 : 22;
        int logoSize = tiny ? 38 : compact ? 48 : 58;
        int buttonWidth = heroWidth;
        int buttonHeight = tiny ? 28 : compact ? 38 : 42;
        int buttonGap = tiny ? 4 : 9;
        int brandHeight = logoSize + (tiny ? 18 : 34);
        int buttonBlockHeight = (PRIMARY_LABELS.length * buttonHeight) + ((PRIMARY_LABELS.length - 1) * buttonGap);
        int heroHeight = heroPaddingTop + brandHeight + buttonBlockHeight + heroPaddingBottom;

        int footerY = screenHeight - margin - (tiny ? 6 : stackedFooter ? 12 : 18);

        boolean showDock = screenWidth >= 540 && screenHeight >= 340;
        int dockButtonWidth = compact ? 104 : 120;
        int dockHeight = showDock ? (tiny ? 24 : 28) : 0;
        int dockGap = tiny ? 6 : 8;
        int dockWidth = showDock ? (DOCK_LABELS.length * dockButtonWidth) + ((DOCK_LABELS.length - 1) * dockGap) : 0;
        int dockX = (screenWidth - dockWidth) / 2;
        int dockY = showDock ? footerY - dockHeight - 20 : 0;

        boolean showStatusCard = wide;
        int statusWidth = showStatusCard ? 252 : 0;
        int statusHeight = showStatusCard ? 82 : 0;
        int statusX = margin;
        int statusY = showStatusCard ? footerY - statusHeight - 10 : 0;

        int topReserved = margin + topButtonHeight + 14;
        int bottomReserved = margin + (tiny ? 40 : stackedFooter ? 32 : 26);
        if (showDock) {
            bottomReserved = Math.max(bottomReserved, screenHeight - dockY + 18);
        }
        if (showStatusCard) {
            bottomReserved = Math.max(bottomReserved, screenHeight - statusY + 18);
        }

        int heroX = (screenWidth - heroWidth) / 2;
        int heroMinY = topReserved;
        int heroMaxY = Math.max(heroMinY, screenHeight - heroHeight - bottomReserved);
        int heroY = clamp((screenHeight - heroHeight) / 2 - (compact ? 4 : 10), heroMinY, heroMaxY);

        return new MenuLayout(
            screenWidth,
            screenHeight,
            margin,
            topRowY,
            heroX,
            heroY,
            heroWidth,
            heroHeight,
            heroRadius,
            heroPaddingX,
            heroPaddingTop,
            logoSize,
            buttonWidth,
            buttonHeight,
            buttonGap,
            socialX,
            socialButtonWidth,
            socialHeight,
            socialGap,
            socialIconOnly,
            utilityX,
            utilityFirstWidth,
            utilitySecondWidth,
            utilityHeight,
            utilityGap,
            showDock,
            dockX,
            dockY,
            dockWidth,
            dockButtonWidth,
            dockHeight,
            dockGap,
            showStatusCard,
            statusX,
            statusY,
            statusWidth,
            statusHeight,
            footerY,
            compact,
            tiny
        );
    }

    @Unique
    private void drawAtmosphere(GuiGraphics graphics, MenuLayout layout, long time) {
        int width = layout.screenWidth();
        int height = layout.screenHeight();
        int driftX = (int) (Math.sin(time * 0.00032D) * 12.0D);
        int driftY = (int) (Math.cos(time * 0.00028D) * 8.0D);

        graphics.fill(0, 0, width, height, 0x58040912);
        drawVerticalGradient(graphics, 0, 0, width, height, 0x1A0B1020, 0xC0060710);
        drawVerticalGradient(graphics, 0, 0, width, Math.max(90, height / 4), 0x68000000, 0x00000000);
        drawVerticalGradient(graphics, 0, height / 2, width, height / 2, 0x00000000, 0x7A04070E);

        drawGlowRect(graphics, (width - 640) / 2 + driftX, (height - 320) / 2 + driftY, 640, 320, 120, 0x148B5CF6);
        drawGlowRect(graphics, -120 + driftX / 2, 24, Math.max(220, width / 3), Math.max(160, height / 4), 110, 0x1038BDF8);
        drawGlowRect(graphics, width - Math.max(260, width / 3) - 40, height - Math.max(180, height / 4) - 20, Math.max(260, width / 3), Math.max(180, height / 4), 120, 0x10F59E0B);

        for (int y = 0; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, 0x03000000);
        }
    }

    @Unique
    private void drawTopRows(GuiGraphics graphics, MenuLayout layout, int mouseX, int mouseY, long time, float introProgress) {
        int rowY = layout.topRowY() + topRowsOffset(layout, introProgress);
        for (int i = 0; i < SOCIAL_LABELS.length; i++) {
            int x = layout.socialButtonX(i);
            boolean hovered = inside(mouseX, mouseY, x, rowY, layout.socialButtonWidth(), layout.socialHeight());
            drawSocialButton(graphics, x, rowY, layout.socialButtonWidth(), layout.socialHeight(), i, hovered, layout.socialIconOnly(), time);
        }

        for (int i = 0; i < UTILITY_LABELS.length; i++) {
            int x = layout.utilityButtonX(i);
            int width = layout.utilityButtonWidth(i);
            boolean hovered = inside(mouseX, mouseY, x, rowY, width, layout.utilityHeight());
            drawUtilityButton(graphics, x, rowY, width, layout.utilityHeight(), i, hovered, time, layout.tiny());
        }
    }

    @Unique
    private void drawHero(GuiGraphics graphics, MenuLayout layout, int mouseX, int mouseY, long time, float introProgress) {
        int heroOffset = heroOffset(layout, introProgress);
        int heroX = layout.heroX();
        int heroY = layout.heroY() + heroOffset;
        int heroWidth = layout.heroWidth();
        int pulseAlpha = 12 + (int) (6.0D * (0.5D + 0.5D * Math.sin(time * 0.0028D)));

        int logoX = heroX + (heroWidth - layout.logoSize()) / 2;
        int logoY = layout.brandTop() + heroOffset;
        int logoFrameSize = layout.logoSize() + (layout.tiny() ? 10 : 14);
        int logoFrameX = heroX + (heroWidth - logoFrameSize) / 2;
        int logoFrameY = logoY - ((logoFrameSize - layout.logoSize()) / 2);
        boolean logoHovered = inside(mouseX, mouseY, logoFrameX, logoFrameY, logoFrameSize, logoFrameSize);
        updateLogoHoverProgress(time, logoHovered);
        float hoverProgress = this.elysia$logoHoverProgress;
        float logoFloat = (float) (Math.sin(time * 0.00185D) * (0.55D + (hoverProgress * 0.85D)));
        float logoLift = -0.85F * hoverProgress;
        float logoOffset = logoLift + logoFloat;
        int hoverAlpha = Math.round(10.0F * hoverProgress);
        int hoverEdge = lerpColor(0x1EFFFFFF, 0x28FFFFFF, hoverProgress);
        int hoverFill = lerpColor(0x620B1020, 0x700B1020, hoverProgress);
        int hoverLine = withAlpha(0x7A8B5CF6, hoverProgress);

        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, logoOffset);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, layout.logoSize(), layout.logoSize());
        graphics.pose().popMatrix();

        String title = "ELYSIA CLIENT";
        if (net.elysiastudios.ElysiaClient.isVIP()) {
            title += "  §b💎";
        }
        int titleX = heroX + (heroWidth - UiFontHelper.width(this.font, title)) / 2;
        UiFontHelper.draw(graphics, this.font, title, titleX, layout.titleY() + heroOffset, 0xFFF8FAFC, true);

        if (!layout.tiny()) {
            String subtitle = "Launcher clean et reactif";
            UiFontHelper.draw(graphics, this.font, subtitle, heroX + (heroWidth - UiFontHelper.width(this.font, subtitle)) / 2, layout.subtitleY() + heroOffset, 0xFF9FB0C8, false);
        }

        for (int i = 0; i < PRIMARY_LABELS.length; i++) {
            int buttonY = layout.buttonY(i) + heroOffset;
            boolean hovered = inside(mouseX, mouseY, layout.buttonX(), buttonY, layout.buttonWidth(), layout.buttonHeight());
            drawPrimaryButton(graphics, layout.buttonX(), buttonY, layout.buttonWidth(), layout.buttonHeight(), i, hovered, layout.tiny(), time);
        }
    }

    @Unique
    private void drawPrimaryButton(GuiGraphics graphics, int x, int y, int width, int height, int index, boolean hovered, boolean compactText, long time) {
        int accent = PRIMARY_ACCENTS[index];
        int border = hovered ? withAlpha(accent, 0.76F) : 0x12FFFFFF;
        int fill = hovered ? 0xD0121828 : 0xA80A1018;

        if (hovered) {
            int alpha = 10 + (int) (8.0D * (0.5D + 0.5D * Math.sin(time * 0.0038D + index)));
            drawGlowRect(graphics, x, y, width, height, height / 2, (alpha << 24) | (accent & 0x00FFFFFF));
        }

        drawRoundedPanel(graphics, x, y, width, height, height / 2, fill, border);
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, Math.max(10, height / 2), 0x18FFFFFF, 0x00000000);
        fillRoundedRect(graphics, x + 11, y + 8, 4, height - 16, 2, hovered ? accent : withAlpha(accent, 0.72F));

        int itemX = x + 24;
        int itemY = y + (height - 16) / 2;
        graphics.renderItem(new ItemStack(iconFor(index)), itemX, itemY);

        if (compactText) {
            String label = trimToWidth(PRIMARY_LABELS[index], width - 74);
            int textY = y + (height - 8) / 2;
            UiFontHelper.draw(graphics, this.font, label, x + 46, textY, 0xFFFFFFFF, true);
            UiFontHelper.draw(graphics, this.font, ">", x + width - 18, textY, hovered ? withAlpha(accent, 0.95F) : 0xFF93A4BF, false);
            return;
        }

        int textX = x + 46;
        int textWidth = Math.max(24, width - 84);
        UiFontHelper.draw(graphics, this.font, trimToWidth(PRIMARY_LABELS[index], textWidth), textX, y + 8, 0xFFFFFFFF, true);
        UiFontHelper.draw(graphics, this.font, trimToWidth(PRIMARY_HINTS[index], textWidth - 12), textX, y + height - 13, hovered ? 0xFFDCE6F7 : 0xFF93A4BF, false);
        UiFontHelper.draw(graphics, this.font, ">", x + width - 18, y + (height - 8) / 2, hovered ? withAlpha(accent, 0.95F) : 0xFF93A4BF, false);
    }

    @Unique
    private Item iconFor(int index) {
        return switch (index) {
            case 0 -> Items.GRASS_BLOCK;
            case 1 -> Items.ENDER_PEARL;
            case 2 -> Items.COMPARATOR;
            case 3 -> Items.BOOKSHELF;
            default -> Items.PAPER;
        };
    }

    @Unique
    private void drawSocialButton(GuiGraphics graphics, int x, int y, int width, int height, int index, boolean hovered, boolean iconOnly, long time) {
        int accent = SOCIAL_ACCENTS[index];

        if (hovered) {
            int alpha = 8 + (int) (5.0D * (0.5D + 0.5D * Math.sin(time * 0.0042D + index)));
            drawGlowRect(graphics, x, y, width, height, Math.max(10, height / 2), (alpha << 24) | (accent & 0x00FFFFFF));
        }

        drawRoundedPanel(
            graphics,
            x,
            y,
            width,
            height,
            Math.max(10, height / 2),
            hovered ? 0xCC111828 : 0xA2090E16,
            hovered ? withAlpha(accent, 0.46F) : 0x12FFFFFF
        );
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, Math.max(7, height / 2), 0x12FFFFFF, 0x00000000);

        int iconBox = height - 8;
        int iconX = iconOnly ? x + (width - iconBox) / 2 : x + 5;
        int iconY = y + 4;
        drawRoundedPanel(
            graphics,
            iconX,
            iconY,
            iconBox,
            iconBox,
            6,
            hovered ? withAlpha(accent, 0.20F) : 0x24070B12,
            hovered ? withAlpha(accent, 0.40F) : 0x18FFFFFF
        );
        int iconSize = Math.max(10, iconBox - 8);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SOCIAL_ICONS[index], iconX + (iconBox - iconSize) / 2, iconY + (iconBox - iconSize) / 2, iconSize, iconSize);

        if (!iconOnly) {
            UiFontHelper.draw(graphics, this.font, SOCIAL_LABELS[index], iconX + iconBox + 6, y + (height - 8) / 2, hovered ? 0xFFFFFFFF : 0xFFDCE6F7, false);
        }
    }

    @Unique
    private void drawUtilityButton(GuiGraphics graphics, int x, int y, int width, int height, int index, boolean hovered, long time, boolean tiny) {
        int accent = UTILITY_ACCENTS[index];
        boolean danger = index == UTILITY_LABELS.length - 1;
        int fill = danger ? (hovered ? 0xD21B1118 : 0xB4131016) : (hovered ? 0xD111192B : 0xB10A0F18);

        if (hovered) {
            int alpha = 10 + (int) (6.0D * (0.5D + 0.5D * Math.sin(time * 0.004D + (index * 0.8D))));
            drawGlowRect(graphics, x, y, width, height, height / 2, (alpha << 24) | (accent & 0x00FFFFFF));
        }

        drawRoundedPanel(graphics, x, y, width, height, height / 2, fill, hovered ? withAlpha(accent, 0.52F) : 0x12FFFFFF);
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, Math.max(8, height / 2), 0x14FFFFFF, 0x00000000);
        String label = trimToWidth(UTILITY_LABELS[index], width - 16);
        UiFontHelper.drawCentered(graphics, this.font, label, x + (width / 2), y + (height - 8) / 2, hovered ? 0xFFFFFFFF : 0xFFDCE6F7, false);
        graphics.fill(x + 8, y + height - 3, x + width - 8, y + height - 2, hovered ? withAlpha(accent, 0.84F) : withAlpha(accent, 0.42F));
    }

    @Unique
    private void drawStatusCard(GuiGraphics graphics, MenuLayout layout, int modsLoaded) {
        if (!layout.showStatusCard()) {
            return;
        }

        int x = layout.statusX();
        int y = layout.statusY();
        int width = layout.statusWidth();
        int height = layout.statusHeight();

        drawGlowRect(graphics, x, y, width, height, 20, 0x1238BDF8);
        drawRoundedPanel(graphics, x, y, width, height, 18, 0xCE0A0F18, 0x22FFFFFF);
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, Math.max(24, height / 2), 0x18FFFFFF, 0x00000000);

        UiFontHelper.draw(graphics, this.font, "SESSION PRETE", x + 18, y + 15, 0xFFF8FAFC, true);
        UiFontHelper.draw(graphics, this.font, trimToWidth("Build stable, modules charges et interface adaptative.", width - 36), x + 18, y + 31, 0xFF9FB0C8, false);

        int chipY = y + height - 27;
        int chipX = x + 18;
        if (net.elysiastudios.ElysiaClient.isVIP()) {
            chipX += drawTagChip(graphics, chipX, chipY, "VIP", 0xFFFACC15) + 8;
        }
        chipX += drawTagChip(graphics, chipX, chipY, "FABRIC", 0xFF38BDF8) + 8;
        chipX += drawTagChip(graphics, chipX, chipY, "QOL", 0xFFA855F7) + 8;
        drawTagChip(graphics, chipX, chipY, modsLoaded + " mods", 0xFFF59E0B);
    }

    @Unique
    private int drawTagChip(GuiGraphics graphics, int x, int y, String label, int accent) {
        int width = UiFontHelper.width(this.font, label) + 18;
        drawRoundedPanel(graphics, x, y, width, 18, 9, withAlpha(accent, 0.16F), withAlpha(accent, 0.42F));
        UiFontHelper.draw(graphics, this.font, label, x + 9, y + 5, 0xFFF8FAFC, false);
        return width;
    }

    @Unique
    private void drawDock(GuiGraphics graphics, MenuLayout layout, int mouseX, int mouseY, long time, float introProgress) {
        if (!layout.showDock()) {
            return;
        }

        int dockY = layout.dockY() + dockOffset(layout, introProgress);
        int railX = layout.dockX() - 6;
        int railY = dockY - 6;
        int hoveredIndex = -1;
        drawRoundedPanel(graphics, railX, railY, layout.dockWidth() + 12, layout.dockHeight() + 12, 14, 0x76070B12, 0x12FFFFFF);

        for (int i = 0; i < DOCK_LABELS.length; i++) {
            int x = layout.dockButtonX(i);
            boolean hovered = inside(mouseX, mouseY, x, dockY, layout.dockButtonWidth(), layout.dockHeight());
            if (hovered) {
                hoveredIndex = i;
            }
            drawDockButton(graphics, x, dockY, layout.dockButtonWidth(), layout.dockHeight(), i, hovered, time);
        }

        if (hoveredIndex >= 0) {
            drawDockTooltip(graphics, layout, hoveredIndex, dockY);
        }
    }

    @Unique
    private void drawDockButton(GuiGraphics graphics, int x, int y, int width, int height, int index, boolean hovered, long time) {
        int accent = DOCK_ACCENTS[index];
        int iconLift = hovered ? -1 - (int) Math.round(Math.sin(time * 0.009D + index) * 1.0D) : 0;
        int glowBonus = hovered && index == 0 ? 2 : 0;
        if (hovered) {
            int alpha = 8 + glowBonus + (int) (6.0D * (0.5D + 0.5D * Math.sin(time * 0.0038D + index)));
            drawGlowRect(graphics, x, y, width, height, 10, (alpha << 24) | (accent & 0x00FFFFFF));
        }

        drawRoundedPanel(graphics, x, y, width, height, 10, hovered ? 0xCC111828 : 0xA2090E16, hovered ? withAlpha(accent, 0.50F) : 0x12FFFFFF);
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, Math.max(8, height / 2), 0x12FFFFFF, 0x00000000);
        graphics.renderItem(new ItemStack(dockIconFor(index)), x + 8, y + (height - 16) / 2 + iconLift);
        String label = trimToWidth(DOCK_LABELS[index], width - 40);
        UiFontHelper.draw(graphics, this.font, label, x + 30, y + (height - 8) / 2, hovered ? 0xFFFFFFFF : 0xFFDCE6F7, false);
        int underlineInset = 8;
        graphics.fill(x + underlineInset, y + height - 3, x + width - underlineInset, y + height - 2, hovered ? withAlpha(accent, index == 0 ? 1.0F : 0.92F) : withAlpha(accent, 0.32F));
    }

    @Unique
    private void drawDockTooltip(GuiGraphics graphics, MenuLayout layout, int index, int dockY) {
        String label = DOCK_LABELS[index];
        String hint = DOCK_HINTS[index];
        String badge = DOCK_BADGES[index];
        int accent = DOCK_ACCENTS[index];
        int badgeWidth = UiFontHelper.width(this.font, badge) + 12;
        int contentWidth = Math.max(UiFontHelper.width(this.font, label), UiFontHelper.width(this.font, hint));
        int width = Math.max(contentWidth + 20, badgeWidth + 20);
        int height = 30;
        int x = clamp(layout.dockButtonX(index) + (layout.dockButtonWidth() - width) / 2, layout.margin(), layout.screenWidth() - layout.margin() - width);
        int y = dockY - 36;

        drawGlowRect(graphics, x, y, width, height, 12, withAlpha(accent, 0.14F));
        drawRoundedPanel(graphics, x, y, width, height, 10, 0xC0070B12, withAlpha(accent, 0.42F));
        drawVerticalGradient(graphics, x + 1, y + 1, width - 2, 12, 0x12FFFFFF, 0x00000000);
        UiFontHelper.draw(graphics, this.font, label, x + 10, y + 6, 0xFFF8FAFC, true);
        UiFontHelper.draw(graphics, this.font, hint, x + 10, y + 18, 0xFF9FB0C8, false);

        int badgeX = x + width - badgeWidth - 6;
        drawRoundedPanel(graphics, badgeX, y + 5, badgeWidth, 14, 7, withAlpha(accent, 0.16F), withAlpha(accent, 0.34F));
        UiFontHelper.draw(graphics, this.font, badge, badgeX + 6, y + 9, 0xFFF8FAFC, false);
    }

    @Unique
    private Item dockIconFor(int index) {
        return switch (index) {
            case 0 -> Items.COMPARATOR;
            case 1 -> Items.ARMOR_STAND;
            default -> Items.PAPER;
        };
    }

    @Unique
    private void drawFooter(GuiGraphics graphics, MenuLayout layout, int modsLoaded, float introProgress) {
        int footerY = layout.footerY() + footerOffset(layout, introProgress);
        String left = "Elysia Client v1.0";
        String right = "MC 1.21.11 | " + modsLoaded + " mods";

        if (layout.screenWidth() < 620) {
            String text = "Elysia Client v1.0 | MC 1.21.11";
            int chipWidth = UiFontHelper.width(this.font, text) + 22;
            drawFooterChip(graphics, (layout.screenWidth() - chipWidth) / 2, footerY, text);
            return;
        }

        drawFooterChip(graphics, layout.margin(), footerY, left);
        int rightWidth = UiFontHelper.width(this.font, right) + 22;
        drawFooterChip(graphics, layout.screenWidth() - layout.margin() - rightWidth, footerY, right);
    }

    @Unique
    private void drawFooterChip(GuiGraphics graphics, int x, int y, String text) {
        int width = UiFontHelper.width(this.font, text) + 22;
        drawRoundedPanel(graphics, x, y, width, 18, 9, 0x84070B12, 0x14FFFFFF);
        UiFontHelper.draw(graphics, this.font, text, x + 11, y + 5, 0xBFE8ECFF, false);
    }

    @Unique
    private void drawIntroOverlay(GuiGraphics graphics, int width, int height, float introProgress) {
        int alpha = Math.max(0, Math.min(255, Math.round((1.0F - introProgress) * 94.0F)));
        if (alpha <= 0) {
            return;
        }
        graphics.fill(0, 0, width, height, (alpha << 24) | 0x050811);
    }

    @Unique
    private float getIntroProgress(long time) {
        if (this.elysia$menuAnimationStart <= 0L) {
            return 1.0F;
        }
        float raw = Math.max(0.0F, Math.min(1.0F, (time - this.elysia$menuAnimationStart) / (float) MENU_INTRO_DURATION_MS));
        return easeOutCubic(raw);
    }

    @Unique
    private float easeOutCubic(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        float inverse = 1.0F - clamped;
        return 1.0F - (inverse * inverse * inverse);
    }

    @Unique
    private int introSlide(float progress, int distance) {
        return Math.round((1.0F - progress) * distance);
    }

    @Unique
    private int topRowsOffset(MenuLayout layout, float progress) {
        return introSlide(progress, layout.tiny() ? 8 : 12);
    }

    @Unique
    private int heroOffset(MenuLayout layout, float progress) {
        return introSlide(progress, layout.tiny() ? 14 : 22);
    }

    @Unique
    private int dockOffset(MenuLayout layout, float progress) {
        return introSlide(progress, layout.tiny() ? 10 : 16);
    }

    @Unique
    private int footerOffset(MenuLayout layout, float progress) {
        return introSlide(progress, layout.tiny() ? 6 : 10);
    }

    @Unique
    private void updateLogoHoverProgress(long time, boolean hovered) {
        if (this.elysia$lastFrameTime < 0L) {
            this.elysia$lastFrameTime = time;
        }

        float deltaSeconds = Math.max(0.0F, Math.min(0.05F, (time - this.elysia$lastFrameTime) / 1000.0F));
        this.elysia$lastFrameTime = time;
        float target = hovered ? 1.0F : 0.0F;
        this.elysia$logoHoverProgress = smoothApproach(this.elysia$logoHoverProgress, target, deltaSeconds, 10.5F);
    }

    @Unique
    private float smoothApproach(float current, float target, float deltaSeconds, float speed) {
        float blend = 1.0F - (float) Math.pow(0.001D, deltaSeconds * speed);
        return current + ((target - current) * blend);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }

        double mouseX = event.x();
        double mouseY = event.y();
        float introProgress = getIntroProgress(System.currentTimeMillis());
        MenuLayout layout = buildLayout(this.width, this.height);
        int heroOffset = heroOffset(layout, introProgress);
        int rowY = layout.topRowY() + topRowsOffset(layout, introProgress);
        int dockY = layout.dockY() + dockOffset(layout, introProgress);
        int logoY = layout.brandTop() + heroOffset;
        int logoFrameSize = layout.logoSize() + (layout.tiny() ? 10 : 14);
        int logoFrameX = layout.heroX() + (layout.heroWidth() - logoFrameSize) / 2;
        int logoFrameY = logoY - ((logoFrameSize - layout.logoSize()) / 2);

        if (inside(mouseX, mouseY, logoFrameX, logoFrameY, logoFrameSize, logoFrameSize)) {
            handleDockAction(0);
            cir.setReturnValue(true);
            return;
        }

        for (int i = 0; i < PRIMARY_LABELS.length; i++) {
            int buttonY = layout.buttonY(i) + heroOffset;
            if (inside(mouseX, mouseY, layout.buttonX(), buttonY, layout.buttonWidth(), layout.buttonHeight())) {
                handlePrimaryAction(i);
                cir.setReturnValue(true);
                return;
            }
        }

        for (int i = 0; i < SOCIAL_LABELS.length; i++) {
            int x = layout.socialButtonX(i);
            if (inside(mouseX, mouseY, x, rowY, layout.socialButtonWidth(), layout.socialHeight())) {
                handleSocialAction(i);
                cir.setReturnValue(true);
                return;
            }
        }

        for (int i = 0; i < UTILITY_LABELS.length; i++) {
            int x = layout.utilityButtonX(i);
            if (inside(mouseX, mouseY, x, rowY, layout.utilityButtonWidth(i), layout.utilityHeight())) {
                handleUtilityAction(i);
                cir.setReturnValue(true);
                return;
            }
        }

        if (layout.showDock()) {
            for (int i = 0; i < DOCK_LABELS.length; i++) {
                int x = layout.dockButtonX(i);
                if (inside(mouseX, mouseY, x, dockY, layout.dockButtonWidth(), layout.dockHeight())) {
                    handleDockAction(i);
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Unique
    private void handlePrimaryAction(int index) {
        switch (index) {
            case 0 -> this.minecraft.setScreen(new SelectWorldScreen(this));
            case 1 -> this.minecraft.setScreen(new JoinMultiplayerScreen(this));
            case 2 -> this.minecraft.setScreen(new ClickGuiScreen(this));
            case 3 -> openModMenu();
            default -> {
            }
        }
    }

    @Unique
    private void handleUtilityAction(int index) {
        switch (index) {
            case 0 -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
            case 1 -> this.minecraft.stop();
            default -> {
            }
        }
    }

    @Unique
    private void handleDockAction(int index) {
        switch (index) {
            case 0 -> this.minecraft.setScreen(new ClickGuiScreen(this));
            case 1 -> this.minecraft.setScreen(new HudEditorScreen(this));
            default -> {
            }
        }
    }

    @Unique
    private void handleSocialAction(int index) {
        Util.getPlatform().openUri(index == 0 ? DISCORD_URL : WEBSITE_URL);
    }

    @Unique
    private void openModMenu() {
        try {
            Class<?> modsScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Object screen = modsScreenClass.getConstructor(Screen.class).newInstance(this);
            this.minecraft.setScreen((Screen) screen);
        } catch (ReflectiveOperationException error) {
            this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
        }
    }

    @Unique
    private void drawGlowRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        int alpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        for (int layer = 3; layer >= 0; layer--) {
            int expand = layer * 12;
            int layerAlpha = Math.max(0, alpha - (layer * 4));
            fillRoundedRect(
                graphics,
                x - expand,
                y - expand,
                width + (expand * 2),
                height + (expand * 2),
                radius + expand,
                (layerAlpha << 24) | rgb
            );
        }
    }

    @Unique
    private void drawRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height, int radius, int fill, int edge) {
        fillRoundedRect(graphics, x, y, width, height, radius, edge);
        fillRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2, Math.max(1, radius - 1), fill);
    }

    @Unique
    private void fillRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int safeRadius = Math.max(1, Math.min(radius, Math.min(width, height) / 2));
        for (int dy = 0; dy < height; dy++) {
            int inset = 0;
            if (dy < safeRadius) {
                double delta = safeRadius - dy - 0.5D;
                inset = Math.max(0, safeRadius - (int) Math.floor(Math.sqrt(Math.max(0.0D, (safeRadius * safeRadius) - (delta * delta)))));
            } else if (dy >= height - safeRadius) {
                double delta = dy - (height - safeRadius) + 0.5D;
                inset = Math.max(0, safeRadius - (int) Math.floor(Math.sqrt(Math.max(0.0D, (safeRadius * safeRadius) - (delta * delta)))));
            }
            graphics.fill(x + inset, y + dy, x + width - inset, y + dy + 1, color);
        }
    }

    @Unique
    private void drawVerticalGradient(GuiGraphics graphics, int x, int y, int width, int height, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        for (int i = 0; i < height; i++) {
            float progress = height <= 1 ? 1.0F : i / (float) (height - 1);
            graphics.fill(x, y + i, x + width, y + i + 1, lerpColor(topColor, bottomColor, progress));
        }
    }

    @Unique
    private void drawHorizontalGradient(GuiGraphics graphics, int x, int y, int width, int height, int leftColor, int rightColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        for (int i = 0; i < width; i++) {
            float progress = width <= 1 ? 1.0F : i / (float) (width - 1);
            graphics.fill(x + i, y, x + i + 1, y + height, lerpColor(leftColor, rightColor, progress));
        }
    }

    @Unique
    private int lerpColor(int from, int to, float progress) {
        float t = Math.max(0.0F, Math.min(1.0F, progress));
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >>> 16) & 0xFF) + ((((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t));
        int g = (int) (((from >>> 8) & 0xFF) + ((((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Unique
    private int withAlpha(int color, float alphaMultiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * alphaMultiplier)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    @Unique
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Unique
    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Unique
    private String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (UiFontHelper.width(this.font, text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - UiFontHelper.width(this.font, ellipsis));
        String trimmed = text;
        while (!trimmed.isEmpty() && UiFontHelper.width(this.font, trimmed) > available) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    @Unique
    private record MenuLayout(
        int screenWidth,
        int screenHeight,
        int margin,
        int topRowY,
        int heroX,
        int heroY,
        int heroWidth,
        int heroHeight,
        int heroRadius,
        int heroPaddingX,
        int heroPaddingTop,
        int logoSize,
        int buttonWidth,
        int buttonHeight,
        int buttonGap,
        int socialX,
        int socialButtonWidth,
        int socialHeight,
        int socialGap,
        boolean socialIconOnly,
        int utilityX,
        int utilityFirstWidth,
        int utilitySecondWidth,
        int utilityHeight,
        int utilityGap,
        boolean showDock,
        int dockX,
        int dockY,
        int dockWidth,
        int dockButtonWidth,
        int dockHeight,
        int dockGap,
        boolean showStatusCard,
        int statusX,
        int statusY,
        int statusWidth,
        int statusHeight,
        int footerY,
        boolean compact,
        boolean tiny
    ) {
        private int brandTop() {
            return heroY + heroPaddingTop;
        }

        private int titleY() {
            return brandTop() + logoSize + (tiny ? 8 : 12);
        }

        private int subtitleY() {
            return titleY() + 14;
        }

        private int buttonX() {
            return heroX + heroPaddingX;
        }

        private int buttonY(int index) {
            return heroY + heroPaddingTop + logoSize + (tiny ? 24 : 40) + (index * (buttonHeight + buttonGap));
        }

        private int socialButtonX(int index) {
            return socialX + (index * (socialButtonWidth + socialGap));
        }

        private int utilityButtonX(int index) {
            return switch (index) {
                case 0 -> utilityX;
                case 1 -> utilityX + utilityFirstWidth + utilityGap;
                default -> utilityX;
            };
        }

        private int utilityButtonWidth(int index) {
            return switch (index) {
                case 0 -> utilityFirstWidth;
                case 1 -> utilitySecondWidth;
                default -> utilitySecondWidth;
            };
        }

        private int dockButtonX(int index) {
            return dockX + (index * (dockButtonWidth + dockGap));
        }
    }
}
