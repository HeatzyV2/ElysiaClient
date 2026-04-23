package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.HudModule;
import net.elysiastudios.client.setting.BooleanModuleSetting;
import net.elysiastudios.client.setting.IntModuleSetting;
import net.minecraft.client.gui.GuiGraphics;

public class WTapHelper extends HudModule {
    private int displayTicks;
    private boolean onlyWhenSprinting;
    private int activeTicks;

    public WTapHelper() {
        super("WTapHelper", "Indicateur visuel pour le W-Tap.", 0, "⌨️");
        this.displayTicks = 12;
        this.onlyWhenSprinting = true;
        registerWTapSettings();
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            activeTicks = 0;
            return;
        }
        if (mc.player.hurtTime > 0 && (!onlyWhenSprinting || mc.player.isSprinting())) {
            activeTicks = displayTicks;
        } else if (activeTicks > 0) {
            activeTicks--;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        if (!isPreviewRender() && activeTicks <= 0) {
            return;
        }

        String text = "RELEASE W";
        int width = measureHudTextWidth(text);
        int height = getHudBoxHeight();
        beginHudRender(guiGraphics);
        drawHudFrame(guiGraphics, width, height, 0xFFEF4444);
        drawHudText(guiGraphics, text, getHudPadding(), getHudTextY(height), 0xFFEF4444);
        endHudRender(guiGraphics);
    }

    private void registerWTapSettings() {
        addSetting(new IntModuleSetting(
            "wtap_display_ticks",
            "Durée",
            "Nombre de ticks pendant lesquels le conseil reste visible.",
            "WTap",
            true,
            () -> displayTicks,
            value -> displayTicks = value,
            12,
            4,
            30,
            1,
            value -> value + " ticks"
        ));
        addSetting(new BooleanModuleSetting(
            "wtap_only_sprinting",
            "Seulement en sprint",
            "Affiche le rappel uniquement pendant les échanges en sprint.",
            "WTap",
            true,
            () -> onlyWhenSprinting,
            value -> onlyWhenSprinting = value,
            true
        ));
    }
}
