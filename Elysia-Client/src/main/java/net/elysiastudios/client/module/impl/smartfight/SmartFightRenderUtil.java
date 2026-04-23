package net.elysiastudios.client.module.impl.smartfight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class SmartFightRenderUtil {
    private SmartFightRenderUtil() {
    }

    static ThemePalette palette(SmartFightSettings.ThemeMode themeMode) {
        return switch (themeMode) {
            case GLACIAL -> new ThemePalette(0xFF38BDF8, 0xFF22D3EE, 0xCC08131D, 0xCC0F1D2B, 0x5538BDF8, 0xFFF8FAFC, 0xFFB6C2D2, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444);
            case EMBER -> new ThemePalette(0xFFF97316, 0xFFFB7185, 0xCC180B08, 0xCC2A130F, 0x55F97316, 0xFFFFF7ED, 0xFFF3D0C2, 0xFF34D399, 0xFFF59E0B, 0xFFEF4444);
            default -> new ThemePalette(0xFF8B5CF6, 0xFF38BDF8, 0xCC0D0A1B, 0xCC17122A, 0x558B5CF6, 0xFFF8FAFC, 0xFFC4C9E4, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444);
        };
    }

    static int withAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int resultAlpha = Math.max(0, Math.min(255, Math.round(alpha * AnalysisMath.clamp(multiplier, 0.0F, 1.0F))));
        return (resultAlpha << 24) | (color & 0x00FFFFFF);
    }

    static void drawRoundedPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int radius, int fill, int border) {
        fillRoundedRect(guiGraphics, x, y, w, h, radius, border);
        fillRoundedRect(guiGraphics, x + 1, y + 1, w - 2, h - 2, Math.max(1, radius - 1), fill);
    }

    static void fillRoundedRect(GuiGraphics guiGraphics, int x, int y, int w, int h, int radius, int color) {
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

    static void drawThinBar(GuiGraphics guiGraphics, int x, int y, int w, int h, float progress, int background, int foreground) {
        guiGraphics.fill(x, y, x + w, y + h, background);
        int fill = Math.max(0, Math.min(w, Math.round(w * AnalysisMath.clamp(progress, 0.0F, 1.0F))));
        if (fill > 0) {
            guiGraphics.fill(x, y, x + fill, y + h, foreground);
        }
    }

    static String percent(float value) {
        return Math.round(AnalysisMath.clamp(value, 0.0F, 1.0F) * 100.0F) + "%";
    }

    static String duration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long mins = seconds / 60L;
        long secs = seconds % 60L;
        return mins > 0L ? mins + "m " + secs + "s" : secs + "s";
    }

    static String distance(double blocks) {
        return String.format(java.util.Locale.ROOT, "%.2f b", blocks);
    }

    static String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    static String trimToPixelWidth(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (Minecraft.getInstance().font.width(value) <= maxWidth) {
            return value;
        }

        String suffix = "...";
        int suffixWidth = Minecraft.getInstance().font.width(suffix);
        if (suffixWidth >= maxWidth) {
            return suffix;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            builder.append(value.charAt(i));
            if (Minecraft.getInstance().font.width(builder.toString()) + suffixWidth > maxWidth) {
                builder.deleteCharAt(builder.length() - 1);
                break;
            }
        }
        return builder + suffix;
    }

    record ThemePalette(
        int accent,
        int accentSoft,
        int background,
        int surface,
        int glow,
        int text,
        int muted,
        int positive,
        int warning,
        int danger
    ) {
    }
}
