package net.elysiastudios.client.module;

import net.elysiastudios.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;

public interface HudWidget {
    String getHudId();

    String getDisplayName();

    int getEditorWidth();

    int getEditorHeight();

    void render(GuiGraphics guiGraphics, boolean preview);

    default int getDefaultX() {
        return 10;
    }

    default int getDefaultY() {
        return 10;
    }

    default float getDefaultScale() {
        return 1.0F;
    }

    default boolean isDefaultVisible() {
        return true;
    }

    default boolean allowMoveInHudEditor() {
        return true;
    }

    default boolean shouldRenderInHudEditor() {
        return true;
    }

    default ConfigManager.HudConfig getConfig() {
        return ConfigManager.getInstance().getHudConfig(getHudId(), getDefaultX(), getDefaultY(), getDefaultScale(), isDefaultVisible());
    }

    default int getX() {
        return getConfig().x;
    }

    default int getY() {
        return getConfig().y;
    }

    default float getScale() {
        return getConfig().scale;
    }

    default boolean isVisible() {
        return getConfig().visible;
    }
}
