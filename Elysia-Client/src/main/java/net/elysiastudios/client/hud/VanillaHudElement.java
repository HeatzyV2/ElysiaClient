package net.elysiastudios.client.hud;

import net.elysiastudios.client.config.ConfigManager;

/**
 * Represents a vanilla HUD element (Scoreboard, Bossbar, Effects, Hotbar, etc.)
 * that can be repositioned and scaled through the HUD editor.
 * 
 * Unlike HudModules, these don't render themselves — they modify the rendering
 * of vanilla Minecraft elements via Mixins using translate/scale on the pose stack.
 */
public class VanillaHudElement {
    private final String id;
    private final String displayName;
    private final String icon;
    private final int defaultWidth;
    private final int defaultHeight;
    private final java.util.function.BiFunction<Integer, Integer, Integer> baseXFunc;
    private final java.util.function.BiFunction<Integer, Integer, Integer> baseYFunc;

    public VanillaHudElement(String id, String displayName, String icon, int defaultWidth, int defaultHeight, 
                             java.util.function.BiFunction<Integer, Integer, Integer> baseXFunc,
                             java.util.function.BiFunction<Integer, Integer, Integer> baseYFunc) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.baseXFunc = baseXFunc;
        this.baseYFunc = baseYFunc;
    }

    public int getBaseX(int screenWidth, int screenHeight) {
        return baseXFunc.apply(screenWidth, screenHeight);
    }

    public int getBaseY(int screenWidth, int screenHeight) {
        return baseYFunc.apply(screenWidth, screenHeight);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getDefaultWidth() { return defaultWidth; }
    public int getDefaultHeight() { return defaultHeight; }

    public ConfigManager.HudConfig getConfig() {
        return ConfigManager.getInstance().getHudConfig(id, 0, 0, 1.0f, true);
    }

    /** Get the effective width at the current scale */
    public int getScaledWidth() {
        return (int) (defaultWidth * getConfig().scale);
    }

    /** Get the effective height at the current scale */
    public int getScaledHeight() {
        return (int) (defaultHeight * getConfig().scale);
    }
}
