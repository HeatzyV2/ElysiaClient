package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

public class ToggleSprintModule extends HudModule {
    private boolean showWalkingState;
    private boolean stopInWater;
    private boolean wasSprinting;

    public ToggleSprintModule() {
        super("ToggleSprint", "Sprint/Sneak permanent", 0, "🏃");
        this.showWalkingState = true;
        this.stopInWater = true;
        registerToggleSprintSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (mc.options.keyUp.isDown() && !mc.player.horizontalCollision && (!stopInWater || !mc.player.isInWater())) {
            mc.player.setSprinting(true);
            wasSprinting = true;
        } else {
            wasSprinting = false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        String text = wasSprinting ? "[Sprinting]" : showWalkingState ? "[Walking]" : "";
        if (text.isEmpty() && !isPreviewRender()) {
            return;
        }
        if (isPreviewRender()) {
            text = "[Sprinting]";
        }

        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        int accentColor = wasSprinting || isPreviewRender() ? 0xFF22C55E : 0xFF94A3B8;

        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, accentColor);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFFFFFFF);
        endHudRender(guiGraphics);
    }

    private void registerToggleSprintSettings() {
        addSetting(new BooleanModuleSetting(
            "toggle_sprint_show_walking",
            "Afficher walking",
            "Affiche aussi l'état walking quand vous ne sprintez pas.",
            "Sprint",
            true,
            () -> showWalkingState,
            value -> showWalkingState = value,
            true
        ));
        addSetting(new BooleanModuleSetting(
            "toggle_sprint_stop_water",
            "Stop dans l'eau",
            "Évite de forcer le sprint dans l'eau.",
            "Sprint",
            true,
            () -> stopInWater,
            value -> stopInWater = value,
            true
        ));
    }
}
