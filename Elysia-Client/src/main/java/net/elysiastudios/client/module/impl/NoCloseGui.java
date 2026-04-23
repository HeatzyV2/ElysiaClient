package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class NoCloseGui extends Module {
    public NoCloseGui() {
        super("NoCloseGui", "Empêche la fermeture des GUIs lors de dégâts.", Category.UTILITY, 0, "🚫");
    }
}
