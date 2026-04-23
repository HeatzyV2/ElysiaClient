package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Freelook extends Module {
    private boolean previousHideGui;

    public Freelook() {
        super("ScreenshotMode", "Masque l'interface pour des captures propres.", Category.RENDER, GLFW.GLFW_KEY_LEFT_ALT, "SHOT");
    }

    @Override
    public void onEnable() {
        previousHideGui = mc.options.hideGui;
        mc.options.hideGui = true;
    }

    @Override
    public void onDisable() {
        mc.options.hideGui = previousHideGui;
    }
}
