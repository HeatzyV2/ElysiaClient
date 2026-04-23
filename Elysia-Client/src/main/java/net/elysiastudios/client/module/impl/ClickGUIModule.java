package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.gui.ClickGuiScreen;
import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public ClickGUIModule() {
        super("ClickGUI", "Ouvre le menu de configuration.", Category.GENERAL, GLFW.GLFW_KEY_RIGHT_SHIFT, "GUI");
    }

    @Override
    public void onEnable() {
        if (mc.screen instanceof ClickGuiScreen) {
            mc.setScreen(null);
        } else {
            mc.setScreen(new ClickGuiScreen());
        }
        setEnabled(false);
    }
}
