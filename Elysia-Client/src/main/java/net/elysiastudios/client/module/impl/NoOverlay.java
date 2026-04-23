package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class NoOverlay extends Module {
    public NoOverlay() {
        super("NoOverlay", "Supprime les overlays (feu, eau).", Category.RENDER, 0, "🖼️");
    }
}
