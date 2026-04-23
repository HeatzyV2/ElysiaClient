package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

public class BuildGuideModule extends HudModule {
    public BuildGuideModule() {
        super("BuildGuide", "Affiche les infos du bloc visé pour la construction.", 0, "BUILD");
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String text;
        if (isPreviewRender()) {
            text = "Block 124 64 -318  Face NORTH";
        } else {
            if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
                return;
            }
            BlockPos pos = blockHitResult.getBlockPos();
            text = "Block " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  Face " + blockHitResult.getDirection();
        }

        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFFF59E0B);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }
}
