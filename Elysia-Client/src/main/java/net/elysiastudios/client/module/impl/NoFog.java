package net.elysiastudios.client.module.impl;

import net.elysiastudios.client.module.Category;
import net.elysiastudios.client.module.Module;

public class NoFog extends Module {
    public NoFog() {
        super("NoFog", "Supprime le brouillard du jeu.", Category.RENDER, 0, "🌫️");
    }
}
