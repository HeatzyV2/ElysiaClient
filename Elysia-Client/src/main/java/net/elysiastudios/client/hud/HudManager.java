package net.elysiastudios.client.hud;

import net.elysiastudios.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.elysiastudios.client.event.ClickTracker;
import net.elysiastudios.client.module.HudWidget;
import net.elysiastudios.client.module.HudWidgetProvider;

public class HudManager {
    public static void init() {
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.screen != null) return;
            
            for (net.elysiastudios.client.module.Module module : net.elysiastudios.client.module.ModuleManager.getInstance().getModules()) {
                if (module.isEnabled() && module instanceof HudWidgetProvider provider) {
                    for (HudWidget widget : provider.getHudWidgets()) {
                        if (widget != null && widget.isVisible()) {
                            widget.render(guiGraphics, false);
                        }
                    }
                }
            }
        });
    }
}
