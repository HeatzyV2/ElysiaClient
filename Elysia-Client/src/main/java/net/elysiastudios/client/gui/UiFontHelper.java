package net.elysiastudios.client.gui;

import net.elysiastudios.ElysiaClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class UiFontHelper {
    private static final FontDescription.Resource DEFAULT_FONT = new FontDescription.Resource(
        Identifier.withDefaultNamespace("default")
    );

    private UiFontHelper() {
    }

    public static int width(Font font, String text) {
        return font.width(asSequence(text));
    }

    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, asSequence(text), x, y, color, shadow);
    }

    public static void drawCentered(GuiGraphics graphics, Font font, String text, int centerX, int y, int color, boolean shadow) {
        draw(graphics, font, text, centerX - (width(font, text) / 2), y, color, shadow);
    }

    public static FormattedCharSequence asSequence(String text) {
        return Component.literal(text == null ? "" : text)
            .withStyle(style -> style.withFont(DEFAULT_FONT))
            .getVisualOrderText();
    }
}
