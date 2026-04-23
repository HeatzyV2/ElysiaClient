package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class NoEntityRender extends Module {
    public NoEntityRender() {
        super("NoEntityRender", "Ne rend pas les entités lointaines.", Category.RENDER, 0, "🚫");
    }
}
