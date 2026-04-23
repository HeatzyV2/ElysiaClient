package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;

public class BiomeModule extends HudModule {
    public BiomeModule() {
        super("Biome", "Affiche le biome et la dimension actuels.", 0, "BIO");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String text;
        if (isPreviewRender()) {
            text = "Plains - Overworld";
        } else {
            if (mc.player == null || mc.level == null) {
                return;
            }
            String biomeName = mc.level.getBiome(mc.player.blockPosition())
                .unwrapKey()
                .map(Object::toString)
                .map(this::formatId)
                .orElse("Unknown biome");
            String dimension = formatId(mc.level.dimension().toString());
            text = biomeName + " - " + dimension;
        }

        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFF4ADE80);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }

    private String formatId(String key) {
        String normalized = key;
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }
        int colonIndex = normalized.lastIndexOf(':');
        if (colonIndex >= 0) {
            normalized = normalized.substring(colonIndex + 1);
        }

        normalized = normalized.replace('[', ' ').replace(']', ' ').replace('/', ' ').trim();
        String[] parts = normalized.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            builder.append(part.substring(1));
        }
        return builder.toString();
    }
}
