package net.elysiastudios.client.module;

import net.minecraft.client.gui.screens.Screen;

public interface ConfigurableModule {
    Screen createConfigScreen(Screen parent);
}
