package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class HitColorModule extends Module {
    public HitColorModule() {
        super("HitColor", "Change la couleur du flash de dégâts.", Category.RENDER, 0, "🔴");
    }
}
