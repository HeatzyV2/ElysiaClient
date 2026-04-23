package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    public Zoom() {
        super("Zoom", "Permet de zoomer (façon OptiFine).", Category.RENDER, GLFW.GLFW_KEY_C);
    }
}
